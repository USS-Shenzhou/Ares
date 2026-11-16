package cn.ussshenzhou.ares.client.charm.physics;

import cn.ussshenzhou.ares.gun.custom.charm.CharmPhysicsSettings;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/// @author TACZ, USS_Shenzhou
public class CharmPhysicsState {
    private static final float MAX_DELTA_SECONDS = 0.05f;
    private static final float MIN_DELTA_SECONDS = 0.001f;
    private static final float DAMPING_REFERENCE_DELTA_SECONDS = 1.0f / 20.0f;
    private static final float MAX_ANCHOR_INERTIA = 32.0f;
    private static final float HARD_TETHER_MULTIPLIER = 3.5f;
    private static final float MAX_CONSTRAINT_CORRECTION_PER_ITERATION = 0.008f;
    private static final float SHALLOW_BOX_COLLISION_CORRECTION_PER_ITERATION = 0.006f;
    private static final float OFFSET_BOX_COLLISION_CORRECTION_PER_ITERATION = 0.009f;
    private static final float DEEP_BOX_COLLISION_CORRECTION_PER_ITERATION = 0.014f;
    private static final float DEEP_BOX_COLLISION_START = 0.024f;
    private static final float DEEP_BOX_COLLISION_FULL = 0.085f;
    private static final float MAX_BOX_CONTACT_SKIN = 0.006f;
    private static final float BOX_CONTACT_SKIN_MULTIPLIER = 0.12f;
    private static final float CONTACT_FACE_REUSE_MARGIN = 0.026f;
    private static final float TRAPPED_CONTACT_MOTION = 0.004f;
    private static final float MIN_SWING_RADIUS_MULTIPLIER = 0.25f;
    private static final float MAX_MIN_SWING_RADIUS = 0.02f;
    private static final float CONTACT_EPSILON = 1.0E-4f;
    private static final float CONTACT_MEMORY_DISTANCE = 0.015f;
    private static final float CONTACT_PENETRATION_MEMORY_DISTANCE = 0.06f;
    private static final float CONTACT_NORMAL_REUSE_DOT = 0.72f;
    private static final float CONTACT_NORMAL_REUSE_BONUS = 0.018f;
    private static final float CONTACT_NORMAL_SWITCH_PENALTY = 0.006f;
    private static final float SPHERE_CONTACT_RADIUS_MULTIPLIER = 0.72f;
    private static final float MIN_SPHERE_CONTACT_RADIUS = 0.006f;
    private static final float MAX_SPHERE_CONTACT_RADIUS = 0.04f;
    private static final float SPHERE_COLLISION_CORRECTION_PER_ITERATION = 0.012f;
    private static final float SWEPT_COLLISION_EXTRA_CORRECTION = 0.004f;
    private static final float SWEEP_EPSILON = 1.0E-5f;

    private final Vector3f position = new Vector3f();
    private final Vector3f previousPosition = new Vector3f();
    private final Vector3f velocityScratch = new Vector3f();
    private final Vector3f previousAnchor = new Vector3f();
    private final Vector3f previousAnchorVelocity = new Vector3f();
    private final Vector3f rememberedContactNormal = new Vector3f();
    private float rememberedContactMinDot;
    private boolean initialized;
    private boolean anchorPositionInitialized;
    private boolean anchorVelocityInitialized;
    private boolean rememberedContact;
    private boolean boxFastCorrectionArmed;
    private boolean currentStepTrappedBoxContact;

    public Vector3f position() {
        return new Vector3f(position);
    }

    public boolean initialized() {
        return initialized;
    }

    public void reset(Vector3f anchor) {
        reset(anchor, CharmPhysicsSettings.DEFAULT);
    }

    public void reset(Vector3f anchor, Vector3f initialPosition) {
        position.set(initialPosition);
        previousPosition.set(initialPosition);
        rememberedContact = false;
        boxFastCorrectionArmed = false;
        currentStepTrappedBoxContact = false;
        initialized = true;
    }

    public void reset(Vector3f anchor, CharmPhysicsSettings settings) {
        position.set(anchor).add(0.0f, -settings.ropeLength(), 0.0f);
        previousPosition.set(position);
        rememberedContact = false;
        boxFastCorrectionArmed = false;
        currentStepTrappedBoxContact = false;
        initialized = true;
    }

    public void step(Vector3f anchor, Vector3f externalAcceleration, CharmPhysicsSettings settings, float deltaSeconds) {
        step(anchor, new Vector3f(anchor).add(0.0f, -settings.ropeLength(), 0.0f), externalAcceleration, settings, deltaSeconds);
    }

    public void step(Vector3f anchor, Vector3f initialPosition, Vector3f externalAcceleration, CharmPhysicsSettings settings, float deltaSeconds) {
        Vector3f totalAcceleration = new Vector3f(externalAcceleration)
                .add(0.0f, -9.81f * settings.gravityScale(), 0.0f);
        stepWithTotalAcceleration(anchor, initialPosition, totalAcceleration, settings, deltaSeconds);
    }

    public void stepWithTotalAcceleration(Vector3f anchor, Vector3f initialPosition, Vector3f totalAcceleration, CharmPhysicsSettings settings, float deltaSeconds) {
        stepWithSurfaceCollision(anchor, initialPosition, totalAcceleration, settings, deltaSeconds, null, null, 0.0f);
    }

    public void stepWithSurfaceCollision(Vector3f anchor, Vector3f initialPosition, Vector3f totalAcceleration, CharmPhysicsSettings settings, float deltaSeconds, Vector3f surfacePoint, Vector3f surfaceNormal, float radius) {
        stepWithCollisionBoxes(anchor, initialPosition, totalAcceleration, settings, deltaSeconds, surfacePoint, surfaceNormal, radius, List.of());
    }

    public void stepWithCollisionBoxes(Vector3f anchor, Vector3f initialPosition, Vector3f totalAcceleration, CharmPhysicsSettings settings, float deltaSeconds, Vector3f surfacePoint, Vector3f surfaceNormal, float radius, List<CharmCollisionBox> collisionBoxes) {
        stepWithCollisionBoxes(anchor, initialPosition, totalAcceleration, settings, deltaSeconds, surfacePoint, surfaceNormal, radius, new Vector3f(), collisionBoxes);
    }

    public void stepWithCollisionBoxes(Vector3f anchor, Vector3f initialPosition, Vector3f totalAcceleration, CharmPhysicsSettings settings, float deltaSeconds, Vector3f surfacePoint, Vector3f surfaceNormal, float radius, Vector3f collisionOffset, List<CharmCollisionBox> collisionBoxes) {
        stepWithCollisionBoxes(anchor, initialPosition, totalAcceleration, settings, deltaSeconds, surfacePoint, surfaceNormal, radius, radius, collisionOffset, collisionBoxes);
    }

    public void stepWithCollisionBoxes(Vector3f anchor, Vector3f initialPosition, Vector3f totalAcceleration, CharmPhysicsSettings settings, float deltaSeconds, Vector3f surfacePoint, Vector3f surfaceNormal, float surfaceRadius, float boxRadius, Vector3f collisionOffset, List<CharmCollisionBox> collisionBoxes) {
        stepWithCollisionProbes(anchor, initialPosition, totalAcceleration, settings, deltaSeconds, surfacePoint, surfaceNormal, surfaceRadius, List.of(new CharmCollisionProbe(collisionOffset, boxRadius)), collisionBoxes);
    }

    public void stepWithCollisionProbes(Vector3f anchor, Vector3f initialPosition, Vector3f totalAcceleration, CharmPhysicsSettings settings, float deltaSeconds, Vector3f surfacePoint, Vector3f surfaceNormal, float surfaceRadius, List<CharmCollisionProbe> collisionProbes, List<CharmCollisionBox> collisionBoxes) {
        float dt = clampedDelta(deltaSeconds);
        currentStepTrappedBoxContact = false;
        if (!initialized || !finite(anchor) || !finite(initialPosition)) {
            reset(anchor, initialPosition);
            return;
        }
        List<CharmCollisionProbe> probes = safeCollisionProbes(collisionProbes);

        velocityScratch.set(position).sub(previousPosition).mul(velocityRetention(settings.damping(), dt));
        previousPosition.set(position);
        position.add(velocityScratch);
        if (finite(totalAcceleration)) {
            position.add(new Vector3f(totalAcceleration).mul(dt * dt));
        }

        ContactPlane contact = rememberedContactPlane();
        ContactPlane activeContact = null;
        for (int i = 0; i < settings.solverIterations(); i++) {
            ContactResult surfaceResult = solveSurfaces(surfacePoint, surfaceNormal, surfaceRadius, probes, contact);
            if (surfaceResult.contact() != null) {
                contact = surfaceResult.contact();
                activeContact = contact;
            }
            ContactResult boxResult = solveBoxes(collisionBoxes, probes, contact);
            if (boxResult.contact() != null) {
                contact = boxResult.contact();
                activeContact = contact;
            }
            ContactResult ropeResult = solveRope(anchor, settings, contact);
            contact = ropeResult.contactOr(contact);
        }
        ContactResult clampResult = hardClamp(anchor, settings, contact);
        contact = clampResult.contactOr(contact);
        ContactResult surfaceResult = solveSurfaces(surfacePoint, surfaceNormal, surfaceRadius, probes, contact);
        if (surfaceResult.contact() != null) {
            contact = surfaceResult.contact();
            activeContact = contact;
        }
        ContactResult boxResult = solveBoxes(collisionBoxes, probes, contact);
        if (boxResult.contact() != null) {
            activeContact = boxResult.contact();
        }
        boxFastCorrectionArmed = currentStepTrappedBoxContact;
        rememberContact(activeContact);
    }

    public Vector3f anchorInertialAcceleration(Vector3f anchor, float deltaSeconds) {
        if (!finite(anchor)) {
            anchorPositionInitialized = false;
            anchorVelocityInitialized = false;
            previousAnchor.zero();
            previousAnchorVelocity.zero();
            return new Vector3f();
        }

        float dt = clampedDelta(deltaSeconds);
        if (!anchorPositionInitialized) {
            previousAnchor.set(anchor);
            previousAnchorVelocity.zero();
            anchorPositionInitialized = true;
            anchorVelocityInitialized = false;
            return new Vector3f();
        }

        Vector3f currentVelocity = new Vector3f(anchor).sub(previousAnchor).div(dt);
        previousAnchor.set(anchor);
        if (!anchorVelocityInitialized) {
            previousAnchorVelocity.set(currentVelocity);
            anchorVelocityInitialized = true;
            return new Vector3f();
        }

        Vector3f acceleration = new Vector3f(currentVelocity).sub(previousAnchorVelocity).div(dt).negate();
        previousAnchorVelocity.set(currentVelocity);
        if (!finite(acceleration)) {
            return new Vector3f();
        }
        float length = acceleration.length();
        if (length > MAX_ANCHOR_INERTIA) {
            acceleration.mul(MAX_ANCHOR_INERTIA / length);
        }
        return acceleration;
    }

    private ContactResult solveRope(Vector3f anchor, CharmPhysicsSettings settings, ContactPlane contact) {
        Vector3f delta = new Vector3f(position).sub(anchor);
        float distance = delta.length();
        float targetDistance = settings.ropeLength();
        Vector3f direction;
        if (distance < 1.0E-5f) {
            direction = swingLimitDirection(anchor, delta);
            distance = 0.0f;
        } else {
            direction = delta.normalize();
        }
        float correction = distance - targetDistance;
        if (Math.abs(correction) <= 1.0E-5f) {
            removeRopeRadialVelocity(anchor);
            return ContactResult.none();
        }
        float stiffness = ropeConstraintStiffness(settings.stiffness());
        if (stiffness <= 1.0E-5f) {
            return ContactResult.none();
        }
        if (contact != null && contact.finite() && correction < 0.0f) {
            removeContactNormalVelocity(contact);
            return ContactResult.blocked(contact);
        }
        Vector3f next = new Vector3f(anchor).add(direction.mul(targetDistance));
        if (stiffness < 1.0f) {
            next = new Vector3f(position).lerp(next, stiffness);
        }
        if (blockedByContact(contact, next)) {
            removeContactNormalVelocity(contact);
            return ContactResult.blocked(contact);
        }
        applyRopeConstraintCorrection(next, anchor, contact == null);
        return ContactResult.none();
    }

    private ContactResult hardClamp(Vector3f anchor, CharmPhysicsSettings settings, ContactPlane contact) {
        if (!finite(position) || !finite(anchor)) {
            reset(anchor, settings);
            return ContactResult.none();
        }
        Vector3f delta = new Vector3f(position).sub(anchor);
        float distance = delta.length();
        float limit = settings.ropeLength() * HARD_TETHER_MULTIPLIER;
        if (distance <= limit) {
            return ContactResult.none();
        }
        Vector3f next;
        if (distance < 1.0E-5f) {
            next = new Vector3f(anchor);
        } else {
            next = new Vector3f(anchor).add(delta.normalize().mul(limit));
        }
        if (blockedByContact(contact, next)) {
            removeContactNormalVelocity(contact);
            return ContactResult.blocked(contact);
        }
        applyConstraintCorrection(next);
        return ContactResult.none();
    }

    private void applyConstraintCorrection(Vector3f next) {
        applyConstraintCorrection(next, Float.POSITIVE_INFINITY);
    }

    private void applyConstraintCorrection(Vector3f next, float maxLength) {
        if (!finite(next)) {
            return;
        }
        Vector3f correction = new Vector3f(next).sub(position);
        if (correction.lengthSquared() < 1.0E-12f) {
            return;
        }
        Vector3f limited = limitedCorrection(correction, maxLength);
        position.add(limited);
        previousPosition.add(limited);
    }

    private void applyRopeConstraintCorrection(Vector3f next, Vector3f anchor, boolean fullCorrection) {
        if (!finite(next)) {
            return;
        }
        Vector3f correction = new Vector3f(next).sub(position);
        if (correction.lengthSquared() < 1.0E-12f) {
            removeRopeRadialVelocity(anchor);
            return;
        }
        if (fullCorrection) {
            position.set(next);
        } else {
            position.add(limitedCorrection(correction, MAX_CONSTRAINT_CORRECTION_PER_ITERATION));
        }
        removeRopeRadialVelocity(anchor);
    }

    private Vector3f swingLimitDirection(Vector3f anchor, Vector3f delta) {
        if (finite(delta) && delta.lengthSquared() > 1.0E-8f) {
            return new Vector3f(delta).normalize();
        }
        Vector3f motion = new Vector3f(position).sub(previousPosition);
        if (finite(motion) && motion.lengthSquared() > 1.0E-8f) {
            return motion.normalize();
        }
        Vector3f previousDelta = new Vector3f(previousPosition).sub(anchor);
        if (finite(previousDelta) && previousDelta.lengthSquared() > 1.0E-8f) {
            return previousDelta.normalize();
        }
        return new Vector3f(0.0f, -1.0f, 0.0f);
    }

    private ContactResult solveSurface(Vector3f surfacePoint, Vector3f surfaceNormal, float radius, Vector3f collisionOffset) {
        if (!finite(position) || surfacePoint == null || surfaceNormal == null || !finite(surfacePoint) || !finite(surfaceNormal) || radius <= 0.0f || surfaceNormal.lengthSquared() < 1.0E-6f) {
            return ContactResult.none();
        }
        Vector3f probe = collisionProbe(collisionOffset);
        Vector3f normal = new Vector3f(surfaceNormal).normalize();
        float distance = new Vector3f(probe).sub(surfacePoint).dot(normal);
        if (distance >= radius) {
            return ContactResult.none();
        }
        Vector3f offset = safeCollisionOffset(collisionOffset);
        ContactPlane contact = new ContactPlane(normal, surfacePoint.dot(normal) + radius - offset.dot(normal));
        applyCollisionCorrection(normal.mul(radius - distance), contact);
        return ContactResult.contact(contact);
    }

    private ContactResult solveSurfaces(Vector3f surfacePoint, Vector3f surfaceNormal, float surfaceRadius, List<CharmCollisionProbe> probes, ContactPlane contact) {
        if (surfaceRadius <= 0.0f) {
            return ContactResult.none();
        }
        ContactResult result = ContactResult.none();
        for (CharmCollisionProbe probe : probes) {
            ContactResult surfaceResult = solveSurface(surfacePoint, surfaceNormal, surfaceRadius, probe.offset());
            if (surfaceResult.contact() != null) {
                result = surfaceResult;
                contact = surfaceResult.contact();
            } else if (surfaceResult.blocked()) {
                result = surfaceResult;
            }
        }
        return result;
    }

    private ContactResult solveBoxes(List<CharmCollisionBox> boxes, float radius, Vector3f collisionOffset, ContactPlane contact) {
        return solveBoxes(boxes, new CharmCollisionProbe(collisionOffset, radius), contact);
    }

    private ContactResult solveBoxes(List<CharmCollisionBox> boxes, List<CharmCollisionProbe> probes, ContactPlane contact) {
        if (!finite(position) || boxes == null || boxes.isEmpty() || probes == null || probes.isEmpty()) {
            return ContactResult.none();
        }
        List<BoxProbeCandidate> candidates = boxProbeCandidates(boxes, probes, contact);
        for (BoxProbeCandidate candidate : candidates) {
            ContactResult result = solveBox(
                    candidate.box(),
                    boxContactSkin(candidate.probe().radius()),
                    boxSphereContactRadius(candidate.probe().radius()),
                    candidate.probe().offset(),
                    contact
            );
            if (result.contact() != null || result.blocked()) {
                return result;
            }
        }
        return ContactResult.none();
    }

    private ContactResult solveBoxes(List<CharmCollisionBox> boxes, CharmCollisionProbe probe, ContactPlane contact) {
        float radius = probe.radius();
        Vector3f collisionOffset = probe.offset();
        if (!finite(position) || boxes == null || boxes.isEmpty() || radius < 0.0f) {
            return ContactResult.none();
        }
        ContactResult result = ContactResult.none();
        float skin = boxContactSkin(radius);
        float sphereRadius = boxSphereContactRadius(radius);
        for (CharmCollisionBox box : boxes) {
            ContactResult boxResult = solveBox(box, skin, sphereRadius, collisionOffset, contact);
            if (boxResult.contact() != null) {
                result = boxResult;
                contact = boxResult.contact();
            }
        }
        return result;
    }

    private static List<CharmCollisionProbe> safeCollisionProbes(List<CharmCollisionProbe> probes) {
        if (probes == null || probes.isEmpty()) {
            return List.of(CharmCollisionProbe.centered(0.0f));
        }
        return probes.stream()
                .filter(probe -> probe != null && finite(probe.offset()) && Float.isFinite(probe.radius()) && probe.radius() >= 0.0f)
                .toList();
    }

    private List<BoxProbeCandidate> boxProbeCandidates(List<CharmCollisionBox> boxes, List<CharmCollisionProbe> probes, ContactPlane contact) {
        List<BoxProbeCandidate> candidates = new ArrayList<>();
        for (CharmCollisionProbe probe : probes) {
            if (probe == null || !finite(probe.offset()) || probe.radius() < 0.0f) {
                continue;
            }
            float skin = boxContactSkin(probe.radius());
            float sphereRadius = boxSphereContactRadius(probe.radius());
            Vector3f offset = probe.offset();
            for (CharmCollisionBox box : boxes) {
                BoxProbeCandidate candidate = boxProbeContactCandidate(box, probe, skin, sphereRadius, offset, contact);
                if (candidate != null && Float.isFinite(candidate.score())) {
                    candidates.add(candidate);
                }
            }
        }
        candidates.sort(Comparator.comparingDouble(BoxProbeCandidate::score));
        return candidates;
    }

    private BoxProbeCandidate boxProbeContactCandidate(CharmCollisionBox box, CharmCollisionProbe probeDefinition, float skin, float sphereRadius, Vector3f offset, ContactPlane contact) {
        if (box == null || !box.finite() || !finite(offset)) {
            return null;
        }
        Vector3f localProbe = box.localPosition(new Vector3f(position).add(offset));
        Vector3f previousProbe = box.localPosition(new Vector3f(previousPosition).add(offset));
        if (!finite(localProbe) || !finite(previousProbe)) {
            return null;
        }
        if (sphereRadius > 0.0f && sweptExpandedBoxFace(box, sphereRadius, previousProbe, localProbe) != null) {
            SweptFace face = sweptExpandedBoxFace(box, sphereRadius, previousProbe, localProbe);
            Vector3f normal = box.worldDirection(localFaceNormal(face.axis(), face.positive()));
            return new BoxProbeCandidate(box, probeDefinition, adjustedContactScore(-sphereRadius - SWEPT_COLLISION_EXTRA_CORRECTION, normal, contact), normal);
        }
        if (box.containsLocal(localProbe)) {
            FaceAxis face = nearestFaceAxis(box, localProbe);
            Vector3f normal = box.worldDirection(localFaceNormal(face.axis(), face.positive()));
            return new BoxProbeCandidate(box, probeDefinition, adjustedContactScore(-nearestFaceDepth(box, localProbe) - skin, normal, contact), normal);
        }
        if (sphereRadius <= 0.0f) {
            return null;
        }
        Vector3f closestLocal = new Vector3f(
                clamp(localProbe.x, box.min().x, box.max().x),
                clamp(localProbe.y, box.min().y, box.max().y),
                clamp(localProbe.z, box.min().z, box.max().z)
        );
        Vector3f probe = new Vector3f(position).add(offset);
        Vector3f closestWorld = box.worldPosition(closestLocal);
        Vector3f delta = new Vector3f(probe).sub(closestWorld);
        float distance = delta.length();
        if (!Float.isFinite(distance) || distance >= sphereRadius || distance <= 1.0E-6f) {
            return null;
        }
        Vector3f normal = delta.mul(1.0f / distance);
        Vector3f velocity = new Vector3f(position).sub(previousPosition);
        if (velocity.dot(normal) >= -1.0E-6f) {
            return null;
        }
        return new BoxProbeCandidate(box, probeDefinition, adjustedContactScore(distance - sphereRadius, normal, contact), normal);
    }

    private float adjustedContactScore(float score, Vector3f normal, ContactPlane contact) {
        if (contact == null || !contact.finite() || !finite(normal) || normal.lengthSquared() < 1.0E-8f) {
            return score;
        }
        Vector3f candidateNormal = new Vector3f(normal).normalize();
        float dot = candidateNormal.dot(contact.normal());
        if (dot >= CONTACT_NORMAL_REUSE_DOT) {
            return score - CONTACT_NORMAL_REUSE_BONUS;
        }
        if (dot >= 0.0f) {
            return score + CONTACT_NORMAL_SWITCH_PENALTY * (1.0f - dot);
        }
        return score + CONTACT_NORMAL_SWITCH_PENALTY;
    }

    private ContactResult solveBox(CharmCollisionBox box, float skin, float sphereRadius, Vector3f collisionOffset, ContactPlane contact) {
        if (box == null || !box.finite()) {
            return ContactResult.none();
        }
        Vector3f probe = collisionProbe(collisionOffset);
        Vector3f localProbe = box.localPosition(probe);
        Vector3f offset = safeCollisionOffset(collisionOffset);
        Vector3f previousProbe = box.localPosition(new Vector3f(previousPosition).add(offset));
        ContactResult sweptResult = solveSweptSphereBox(box, sphereRadius, offset, localProbe, previousProbe, contact);
        if (sweptResult.contact() != null) {
            return sweptResult;
        }
        if (box.containsLocal(localProbe)) {
            return pushFromInsideBox(box, skin, collisionOffset, localProbe, contact);
        }
        ContactResult sphereResult = solveSphereBox(box, sphereRadius, offset, localProbe, contact);
        if (sphereResult.contact() != null) {
            return sphereResult;
        }
        return ContactResult.none();
    }

    private ContactResult solveSweptSphereBox(CharmCollisionBox box, float radius, Vector3f offset, Vector3f localProbe, Vector3f previousProbe, ContactPlane contact) {
        if (radius <= 0.0f || !finite(localProbe) || !finite(previousProbe)) {
            return ContactResult.none();
        }
        SweptFace face = sweptExpandedBoxFace(box, radius, previousProbe, localProbe);
        if (face == null) {
            return ContactResult.none();
        }
        FaceCorrection correction = faceCorrection(box, radius + SWEPT_COLLISION_EXTRA_CORRECTION, offset, face.axis(), face.positive(), true);
        if (blockedByContact(contact, new Vector3f(position).add(correction.correction()))) {
            removeContactNormalVelocity(contact);
            return ContactResult.blocked(contact);
        }
        applyCollisionCorrection(correction.correction(), correction.contact(), Float.POSITIVE_INFINITY);
        return ContactResult.contact(correction.contact());
    }

    private ContactResult solveSphereBox(CharmCollisionBox box, float radius, Vector3f offset, Vector3f localProbe, ContactPlane contact) {
        if (radius <= 0.0f || !finite(localProbe)) {
            return ContactResult.none();
        }
        Vector3f closestLocal = new Vector3f(
                clamp(localProbe.x, box.min().x, box.max().x),
                clamp(localProbe.y, box.min().y, box.max().y),
                clamp(localProbe.z, box.min().z, box.max().z)
        );
        Vector3f probe = new Vector3f(position).add(offset);
        Vector3f closestWorld = box.worldPosition(closestLocal);
        Vector3f delta = new Vector3f(probe).sub(closestWorld);
        float distance = delta.length();
        if (!Float.isFinite(distance) || distance >= radius || distance <= 1.0E-6f) {
            return ContactResult.none();
        }
        Vector3f normal = delta.mul(1.0f / distance);
        Vector3f velocity = new Vector3f(position).sub(previousPosition);
        if (velocity.dot(normal) >= -1.0E-6f) {
            return ContactResult.none();
        }
        Vector3f correction = new Vector3f(normal).mul(radius - distance);
        ContactPlane plane = new ContactPlane(normal, closestWorld.dot(normal) + radius - offset.dot(normal));
        if (blockedByContact(contact, new Vector3f(position).add(correction))) {
            removeContactNormalVelocity(contact);
            return ContactResult.blocked(contact);
        }
        applyCollisionCorrection(correction, plane, SPHERE_COLLISION_CORRECTION_PER_ITERATION);
        return ContactResult.contact(plane);
    }

    private ContactResult pushFromInsideBox(CharmCollisionBox box, float skin, Vector3f collisionOffset, Vector3f localProbe, ContactPlane contact) {
        Vector3f offset = safeCollisionOffset(collisionOffset);
        Vector3f previousProbe = box.localPosition(new Vector3f(previousPosition).add(offset));
        boolean trappedContact = trappedContact(localProbe, previousProbe) && offset.lengthSquared() < 1.0E-8f;
        if (trappedContact) {
            currentStepTrappedBoxContact = true;
        }
        boolean fastCorrection = boxFastCorrectionArmed && trappedContact;
        FaceCorrection face = contactFaceCorrection(box, skin, offset, contact, localProbe);
        Vector3f correction = face == null ? null : face.correction();
        if (correction == null) {
            face = entryFaceCorrection(box, skin, offset, localProbe, previousProbe);
            correction = face == null ? null : face.correction();
        }
        if (correction == null) {
            face = motionFaceCorrection(box, skin, offset, localProbe, previousProbe);
            correction = face == null ? null : face.correction();
        }
        if (correction == null) {
            face = offsetFaceCorrection(box, skin, offset);
            correction = face == null ? null : face.correction();
        }
        if (correction == null) {
            face = nearestFaceCorrection(box, skin, offset, localProbe);
            if (fastCorrection) {
                face = face.withAcceleration();
            }
            correction = face.correction();
        } else if (fastCorrection && face.canAccelerate()) {
            face = face.withAcceleration();
            correction = face.correction();
        }
        float limit;
        if (face.fastCorrection()) {
            limit = boxCollisionCorrectionLimit(correction);
        } else if (offset.lengthSquared() > 1.0E-8f) {
            limit = Math.min(boxCollisionCorrectionLimit(correction), OFFSET_BOX_COLLISION_CORRECTION_PER_ITERATION);
        } else {
            limit = SHALLOW_BOX_COLLISION_CORRECTION_PER_ITERATION;
        }
        applyCollisionCorrection(correction, face.contact(), limit);
        return ContactResult.contact(face.contact());
    }

    private FaceCorrection contactFaceCorrection(CharmCollisionBox box, float skin, Vector3f offset, ContactPlane contact, Vector3f probe) {
        if (contact == null || !contact.finite()) {
            return null;
        }
        Vector3f normal = box.localDirection(contact.normal());
        if (!finite(normal) || normal.lengthSquared() < 1.0E-8f) {
            return null;
        }
        normal.normalize();
        float absX = Math.abs(normal.x);
        float absY = Math.abs(normal.y);
        float absZ = Math.abs(normal.z);
        int axis = 0;
        float component = normal.x;
        float best = absX;
        if (absY > best) {
            axis = 1;
            component = normal.y;
            best = absY;
        }
        if (absZ > best) {
            axis = 2;
            component = normal.z;
            best = absZ;
        }
        if (best < 0.75f) {
            return null;
        }
        boolean positive = component > 0.0f;
        float selectedDepth = faceDepth(box, probe, axis, positive);
        float nearestDepth = nearestFaceDepth(box, probe);
        if (Float.isFinite(selectedDepth) && Float.isFinite(nearestDepth)
                && selectedDepth > nearestDepth + CONTACT_FACE_REUSE_MARGIN) {
            return null;
        }
        return faceCorrection(box, skin, offset, axis, positive, true);
    }

    private FaceCorrection entryFaceCorrection(CharmCollisionBox box, float skin, Vector3f offset, Vector3f probe, Vector3f previousProbe) {
        if (!finite(previousProbe) || box.containsLocal(previousProbe)) {
            return null;
        }
        Vector3f motion = new Vector3f(probe).sub(previousProbe);
        if (motion.lengthSquared() < 1.0E-10f) {
            return null;
        }

        EntryFace best = null;
        best = crossedEntryFace(best, previousProbe.x, probe.x, motion.x, box.min().x, 0, false);
        best = crossedEntryFace(best, previousProbe.x, probe.x, motion.x, box.max().x, 0, true);
        best = crossedEntryFace(best, previousProbe.y, probe.y, motion.y, box.min().y, 1, false);
        best = crossedEntryFace(best, previousProbe.y, probe.y, motion.y, box.max().y, 1, true);
        best = crossedEntryFace(best, previousProbe.z, probe.z, motion.z, box.min().z, 2, false);
        best = crossedEntryFace(best, previousProbe.z, probe.z, motion.z, box.max().z, 2, true);
        if (best == null) {
            return null;
        }
        return faceCorrection(box, skin, offset, best.axis(), best.positive());
    }

    private EntryFace crossedEntryFace(EntryFace best, float previous, float current, float motion, float plane, int axis, boolean positive) {
        if (Math.abs(motion) < 1.0E-8f) {
            return best;
        }
        if (positive) {
            if (!(previous > plane && current <= plane)) {
                return best;
            }
        } else if (!(previous < plane && current >= plane)) {
            return best;
        }

        float t = (plane - previous) / motion;
        if (t < -1.0E-5f || t > 1.00001f || !Float.isFinite(t)) {
            return best;
        }
        if (best == null || t > best.t()) {
            return new EntryFace(axis, positive, t);
        }
        return best;
    }

    private FaceCorrection motionFaceCorrection(CharmCollisionBox box, float skin, Vector3f offset, Vector3f probe, Vector3f previousProbe) {
        if (!finite(previousProbe)) {
            return null;
        }
        Vector3f motion = new Vector3f(probe).sub(previousProbe);
        float absX = Math.abs(motion.x);
        float absY = Math.abs(motion.y);
        float absZ = Math.abs(motion.z);
        if (absX < 1.0E-8f && absY < 1.0E-8f && absZ < 1.0E-8f) {
            return null;
        }

        int axis = 0;
        float component = motion.x;
        float best = absX;
        if (absY > best) {
            axis = 1;
            component = motion.y;
            best = absY;
        }
        if (absZ > best) {
            axis = 2;
            component = motion.z;
        }
        return faceCorrection(box, skin, offset, axis, component < 0.0f);
    }

    private FaceCorrection offsetFaceCorrection(CharmCollisionBox box, float skin, Vector3f offset) {
        Vector3f localOffset = box.localDirection(offset);
        if (!finite(localOffset) || localOffset.lengthSquared() < 1.0E-8f) {
            return null;
        }
        float absX = Math.abs(localOffset.x);
        float absY = Math.abs(localOffset.y);
        float absZ = Math.abs(localOffset.z);
        int axis = 0;
        float component = localOffset.x;
        float best = absX;
        if (absY > best) {
            axis = 1;
            component = localOffset.y;
            best = absY;
        }
        if (absZ > best) {
            axis = 2;
            component = localOffset.z;
        }
        return faceCorrection(box, skin, offset, axis, component > 0.0f);
    }

    private FaceCorrection nearestFaceCorrection(CharmCollisionBox box, float skin, Vector3f offset, Vector3f probe) {
        FaceAxis face = nearestFaceAxis(box, probe);
        return faceCorrection(box, skin, offset, face.axis(), face.positive(), true);
    }

    private FaceAxis nearestFaceAxis(CharmCollisionBox box, Vector3f probe) {
        float minX = Math.abs(probe.x - box.min().x);
        float maxX = Math.abs(box.max().x - probe.x);
        float minY = Math.abs(probe.y - box.min().y);
        float maxY = Math.abs(box.max().y - probe.y);
        float minZ = Math.abs(probe.z - box.min().z);
        float maxZ = Math.abs(box.max().z - probe.z);

        float best = minX;
        int axis = 0;
        boolean positive = false;
        if (maxX < best) {
            best = maxX;
            positive = true;
        }
        if (minY < best) {
            best = minY;
            axis = 1;
            positive = false;
        }
        if (maxY < best) {
            best = maxY;
            axis = 1;
            positive = true;
        }
        if (minZ < best) {
            best = minZ;
            axis = 2;
            positive = false;
        }
        if (maxZ < best) {
            axis = 2;
            positive = true;
        }

        return new FaceAxis(axis, positive);
    }

    private static Vector3f localFaceNormal(int axis, boolean positive) {
        Vector3f normal = new Vector3f();
        if (axis == 0) {
            normal.x = positive ? 1.0f : -1.0f;
        } else if (axis == 1) {
            normal.y = positive ? 1.0f : -1.0f;
        } else {
            normal.z = positive ? 1.0f : -1.0f;
        }
        return normal;
    }

    private FaceCorrection faceCorrection(CharmCollisionBox box, float skin, Vector3f offset, int axis, boolean positive) {
        return faceCorrection(box, skin, offset, axis, positive, false);
    }

    private FaceCorrection faceCorrection(CharmCollisionBox box, float skin, Vector3f offset, int axis, boolean positive, boolean canAccelerate) {
        Vector3f localProbe = box.localPosition(new Vector3f(position).add(offset));
        Vector3f localNext = new Vector3f(localProbe);
        Vector3f localSurface = new Vector3f(localProbe);
        Vector3f localNormal = new Vector3f();
        if (axis == 0) {
            float surface = positive ? box.max().x : box.min().x;
            localSurface.x = surface;
            localNext.x = surface + (positive ? skin : -skin);
            localNormal.x = positive ? 1.0f : -1.0f;
        } else if (axis == 1) {
            float surface = positive ? box.max().y : box.min().y;
            localSurface.y = surface;
            localNext.y = surface + (positive ? skin : -skin);
            localNormal.y = positive ? 1.0f : -1.0f;
        } else {
            float surface = positive ? box.max().z : box.min().z;
            localSurface.z = surface;
            localNext.z = surface + (positive ? skin : -skin);
            localNormal.z = positive ? 1.0f : -1.0f;
        }
        Vector3f targetProbe = box.worldPosition(localNext);
        Vector3f surfaceProbe = box.worldPosition(localSurface);
        Vector3f targetPosition = targetProbe.sub(offset);
        Vector3f worldNormal = box.worldDirection(localNormal);
        if (worldNormal.lengthSquared() > 1.0E-8f) {
            worldNormal.normalize();
        }
        return new FaceCorrection(targetPosition.sub(position), new ContactPlane(worldNormal, surfaceProbe.dot(worldNormal)), false, canAccelerate);
    }

    private void applyCollisionCorrection(Vector3f correction, ContactPlane contact) {
        applyCollisionCorrection(correction, contact, Float.POSITIVE_INFINITY);
    }

    private void applyCollisionCorrection(Vector3f correction, ContactPlane contact, float maxLength) {
        if (!finite(correction) || correction.lengthSquared() < 1.0E-12f) {
            return;
        }
        Vector3f limited = limitedCorrection(correction, maxLength);
        position.add(limited);
        previousPosition.add(limited);
        removeContactNormalVelocity(contact);
    }

    private ContactPlane rememberedContactPlane() {
        if (!rememberedContact || rememberedContactNormal.lengthSquared() < 1.0E-6f) {
            return null;
        }
        ContactPlane contact = new ContactPlane(new Vector3f(rememberedContactNormal), rememberedContactMinDot);
        float separation = contact.side(position);
        if (separation < -CONTACT_PENETRATION_MEMORY_DISTANCE || separation > CONTACT_MEMORY_DISTANCE) {
            rememberedContact = false;
            return null;
        }
        return contact;
    }

    private void rememberContact(ContactPlane contact) {
        if (contact == null || !contact.finite()) {
            rememberedContact = false;
            return;
        }
        float separation = contact.side(position);
        if (separation < -CONTACT_PENETRATION_MEMORY_DISTANCE || separation > CONTACT_MEMORY_DISTANCE) {
            rememberedContact = false;
            return;
        }
        rememberedContactNormal.set(contact.normal());
        rememberedContactMinDot = contact.minDot();
        rememberedContact = true;
    }

    private boolean blockedByContact(ContactPlane contact, Vector3f candidate) {
        if (contact == null || !contact.finite() || !finite(candidate)) {
            return false;
        }
        float currentSide = contact.side(position);
        float candidateSide = contact.side(candidate);
        return currentSide <= CONTACT_MEMORY_DISTANCE
                && currentSide >= -CONTACT_PENETRATION_MEMORY_DISTANCE
                && candidateSide < currentSide - CONTACT_EPSILON;
    }

    private void removeContactNormalVelocity(ContactPlane contact) {
        if (contact == null || !contact.finite()) {
            return;
        }
        Vector3f velocity = new Vector3f(position).sub(previousPosition);
        float normalVelocity = velocity.dot(contact.normal());
        if (normalVelocity >= -1.0E-8f) {
            return;
        }
        previousPosition.add(new Vector3f(contact.normal()).mul(normalVelocity));
    }

    private void removeRopeRadialVelocity(Vector3f anchor) {
        if (!finite(anchor) || !finite(position) || !finite(previousPosition)) {
            return;
        }
        Vector3f normal = new Vector3f(position).sub(anchor);
        if (normal.lengthSquared() < 1.0E-8f) {
            return;
        }
        normal.normalize();
        Vector3f velocity = new Vector3f(position).sub(previousPosition);
        float radialVelocity = velocity.dot(normal);
        if (Math.abs(radialVelocity) < 1.0E-8f) {
            return;
        }
        previousPosition.add(normal.mul(radialVelocity));
    }

    private static Vector3f limitedCorrection(Vector3f correction, float maxLength) {
        Vector3f limited = new Vector3f(correction);
        if (!Float.isFinite(maxLength) || maxLength <= 0.0f) {
            return limited;
        }
        float length = limited.length();
        if (length > maxLength) {
            limited.mul(maxLength / length);
        }
        return limited;
    }

    private static float boxCollisionCorrectionLimit(Vector3f correction) {
        if (!finite(correction)) {
            return SHALLOW_BOX_COLLISION_CORRECTION_PER_ITERATION;
        }
        float length = correction.length();
        if (length <= DEEP_BOX_COLLISION_START) {
            return SHALLOW_BOX_COLLISION_CORRECTION_PER_ITERATION;
        }
        float t = clamp(
                (length - DEEP_BOX_COLLISION_START) / (DEEP_BOX_COLLISION_FULL - DEEP_BOX_COLLISION_START),
                0.0f,
                1.0f
        );
        return SHALLOW_BOX_COLLISION_CORRECTION_PER_ITERATION
                + (DEEP_BOX_COLLISION_CORRECTION_PER_ITERATION - SHALLOW_BOX_COLLISION_CORRECTION_PER_ITERATION) * t;
    }

    private static float faceDepth(CharmCollisionBox box, Vector3f probe, int axis, boolean positive) {
        if (box == null || probe == null) {
            return Float.POSITIVE_INFINITY;
        }
        if (axis == 0) {
            return positive ? box.max().x - probe.x : probe.x - box.min().x;
        }
        if (axis == 1) {
            return positive ? box.max().y - probe.y : probe.y - box.min().y;
        }
        return positive ? box.max().z - probe.z : probe.z - box.min().z;
    }

    private static float nearestFaceDepth(CharmCollisionBox box, Vector3f probe) {
        if (box == null || probe == null) {
            return Float.POSITIVE_INFINITY;
        }
        float nearest = faceDepth(box, probe, 0, false);
        nearest = Math.min(nearest, faceDepth(box, probe, 0, true));
        nearest = Math.min(nearest, faceDepth(box, probe, 1, false));
        nearest = Math.min(nearest, faceDepth(box, probe, 1, true));
        nearest = Math.min(nearest, faceDepth(box, probe, 2, false));
        return Math.min(nearest, faceDepth(box, probe, 2, true));
    }

    private static boolean trappedContact(Vector3f probe, Vector3f previousProbe) {
        return finite(probe)
                && finite(previousProbe)
                && probe.distanceSquared(previousProbe) <= TRAPPED_CONTACT_MOTION * TRAPPED_CONTACT_MOTION;
    }

    private static float boxContactSkin(float radius) {
        if (!Float.isFinite(radius) || radius <= 0.0f) {
            return CONTACT_EPSILON;
        }
        return Math.min(radius * BOX_CONTACT_SKIN_MULTIPLIER, MAX_BOX_CONTACT_SKIN);
    }

    private static float boxSphereContactRadius(float radius) {
        if (!Float.isFinite(radius) || radius <= 0.0f) {
            return 0.0f;
        }
        return clamp(radius * SPHERE_CONTACT_RADIUS_MULTIPLIER, MIN_SPHERE_CONTACT_RADIUS, MAX_SPHERE_CONTACT_RADIUS);
    }

    private static SweptFace sweptExpandedBoxFace(CharmCollisionBox box, float radius, Vector3f previous, Vector3f current) {
        Vector3f min = new Vector3f(box.min()).sub(radius, radius, radius);
        Vector3f max = new Vector3f(box.max()).add(radius, radius, radius);
        if (insideExpanded(previous, min, max) || insideExpanded(current, min, max)) {
            return null;
        }

        float tMin = 0.0f;
        float tMax = 1.0f;
        int hitAxis = -1;
        boolean hitPositive = false;
        Vector3f motion = new Vector3f(current).sub(previous);
        for (int axis = 0; axis < 3; axis++) {
            float origin = component(previous, axis);
            float direction = component(motion, axis);
            float minValue = component(min, axis);
            float maxValue = component(max, axis);
            if (Math.abs(direction) < SWEEP_EPSILON) {
                if (origin < minValue || origin > maxValue) {
                    return null;
                }
                continue;
            }
            float invDirection = 1.0f / direction;
            float t1 = (minValue - origin) * invDirection;
            float t2 = (maxValue - origin) * invDirection;
            boolean positive = false;
            if (t1 > t2) {
                float swap = t1;
                t1 = t2;
                t2 = swap;
                positive = true;
            }
            if (t1 > tMin) {
                tMin = t1;
                hitAxis = axis;
                hitPositive = positive;
            }
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) {
                return null;
            }
        }
        if (hitAxis < 0 || tMin < -SWEEP_EPSILON || tMin > 1.0f + SWEEP_EPSILON) {
            return null;
        }
        return new SweptFace(hitAxis, hitPositive);
    }

    private static boolean insideExpanded(Vector3f point, Vector3f min, Vector3f max) {
        return point.x >= min.x && point.x <= max.x
                && point.y >= min.y && point.y <= max.y
                && point.z >= min.z && point.z <= max.z;
    }

    private static float component(Vector3f vector, int axis) {
        if (axis == 0) {
            return vector.x;
        }
        if (axis == 1) {
            return vector.y;
        }
        return vector.z;
    }

    private Vector3f collisionProbe(Vector3f collisionOffset) {
        return new Vector3f(position).add(safeCollisionOffset(collisionOffset));
    }

    private static Vector3f safeCollisionOffset(Vector3f collisionOffset) {
        if (collisionOffset == null || !finite(collisionOffset)) {
            return new Vector3f();
        }
        return new Vector3f(collisionOffset);
    }

    private static boolean finite(Vector3f value) {
        return Float.isFinite(value.x) && Float.isFinite(value.y) && Float.isFinite(value.z);
    }

    private static float clampedDelta(float deltaSeconds) {
        return Math.max(MIN_DELTA_SECONDS, Math.min(deltaSeconds, MAX_DELTA_SECONDS));
    }

    public static float velocityRetention(float damping, float deltaSeconds) {
        float baseRetention = clamp(1.0f - damping, 0.0f, 1.0f);
        float normalizedStep = clampedDelta(deltaSeconds) / DAMPING_REFERENCE_DELTA_SECONDS;
        return (float) Math.pow(baseRetention, normalizedStep);
    }

    private static float ropeConstraintStiffness(float stiffness) {
        return clamp(stiffness, 0.0f, 1.0f);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }

    private record EntryFace(int axis, boolean positive, float t) {
    }

    private record SweptFace(int axis, boolean positive) {
    }

    private record FaceAxis(int axis, boolean positive) {
    }

    private record BoxProbeCandidate(CharmCollisionBox box, CharmCollisionProbe probe, float score, Vector3f normal) {
        private BoxProbeCandidate {
            normal = normal == null ? new Vector3f() : new Vector3f(normal);
        }
    }

    private record FaceCorrection(Vector3f correction, ContactPlane contact, boolean fastCorrection, boolean canAccelerate) {
        private FaceCorrection(Vector3f correction, ContactPlane contact) {
            this(correction, contact, false, false);
        }

        private FaceCorrection {
            correction = new Vector3f(correction);
        }

        private FaceCorrection withAcceleration() {
            return new FaceCorrection(correction, contact, true, canAccelerate);
        }
    }

    private record ContactPlane(Vector3f normal, float minDot) {
        private ContactPlane {
            normal = new Vector3f(normal);
            if (normal.lengthSquared() > 1.0E-6f) {
                normal.normalize();
            }
        }

        float side(Vector3f point) {
            return new Vector3f(point).dot(normal) - minDot;
        }

        boolean finite() {
            return CharmPhysicsState.finite(normal) && Float.isFinite(minDot) && normal.lengthSquared() > 1.0E-6f;
        }
    }

    private record ContactResult(ContactPlane contact, boolean blocked) {
        private static ContactResult none() {
            return new ContactResult(null, false);
        }

        private static ContactResult contact(ContactPlane contact) {
            return new ContactResult(contact, false);
        }

        private static ContactResult blocked(ContactPlane contact) {
            return new ContactResult(contact, true);
        }

        private ContactPlane contactOr(ContactPlane fallback) {
            return contact == null ? fallback : contact;
        }
    }
}
