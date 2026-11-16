package cn.ussshenzhou.ares.gun.util.math;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/// @author TACZ, USS_Shenzhou
public class SecondOrderDynamics {
    public static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final float k1;
    private final float k2;
    private final float k3;

    private final float[] py;
    private final float[] pyd;
    private final float[] px;

    private final float[] target;

    private ScheduledFuture<?> handle;

    /// > @param f  Natural frequency
    /// >
    /// > @param z  Damping coefficient
    /// >
    /// > @param r  Initial velocity
    /// >
    /// > @param x0 Initial position
    public SecondOrderDynamics(float f, float z, float r, float[] x0) {
        k1 = (float) (z / (Math.PI * f));
        k2 = (float) (1 / ((2 * Math.PI * f) * (2 * Math.PI * f)));
        k3 = (float) (r * z / (2 * Math.PI * f));

        py = Arrays.copyOf(x0, x0.length);
        px = Arrays.copyOf(x0, x0.length);
        pyd = new float[py.length];

        target = Arrays.copyOf(x0, x0.length);

        handle = scheduler.scheduleAtFixedRate(this::update, 0, 5, TimeUnit.MILLISECONDS);
    }

    public void target(int i, float t) {
        if (handle == null) {
            throw new IllegalStateException("this dynamic has already been canceled");
        }
        target[i] = t;
    }

    public float get(int i) {
        if (handle == null) {
            throw new IllegalStateException("this dynamic has already been canceled");
        }
        /// > 修正罕见的 NAN 错误
        if (Float.isNaN(py[i])) {
            py[i] = 0;
        }
        if (Float.isNaN(pyd[i])) {
            pyd[i] = 0;
        }
        return py[i] + 0.05f * pyd[i];
    }

    public boolean isStable() {
        for (int i = 0; i < target.length; i++) {
            if (Math.abs(target[i] - get(i)) > Math.max(1e-3f, Math.ulp(target[i]))) {
                return false;
            }
        }
        return true;
    }

    public void cancel() {
        if (handle == null) {
            throw new IllegalStateException("this dynamic has already been canceled");
        }
        handle.cancel(false);
        handle = null;
    }

    private void update() {
        for (int i = 0; i < py.length; i++) {
            /// > 修正罕见的 NAN 错误
            if (Float.isNaN(py[i])) {
                py[i] = 0;
            }
            if (Float.isNaN(pyd[i])) {
                pyd[i] = 0;
            }

            float t = 0.05f;
            float xd = (target[i] - px[i]) / t;
            float y = py[i] + t * pyd[i];

            pyd[i] = pyd[i] + t * (px[i] + k3 * xd - py[i] - k1 * pyd[i]) / k2;
            px[i] = target[i];
            py[i] = y;
        }
    }
}
