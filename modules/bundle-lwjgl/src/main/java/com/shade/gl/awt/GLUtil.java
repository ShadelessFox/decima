package com.shade.gl.awt;

import com.shade.gl.awt.GLData.API;

class GLUtil {
    
    static boolean atLeast32(int major, int minor) {
        return major == 3 && minor >= 2 || major > 3;
    }

    static boolean atLeast30(int major, int minor) {
        return major == 3 && minor >= 0 || major > 3;
    }

    static boolean validVersionGL(int major, int minor) {
        return (major == 0 && minor == 0)
                || // unspecified gets highest supported version on Nvidia
                (major >= 1 && minor >= 0) && (major != 1 || minor <= 5) && (major != 2 || minor <= 1) && (major != 3 || minor <= 3)
                && (major != 4 || minor <= 5);
    }

    public static boolean validVersionGLES(int major, int minor) {
        return (major == 0 && minor == 0) || // unspecified gets 1.1 on Nvidia
                (major >= 1 && minor >= 0) && (major != 1 || minor <= 1) && (major != 2 || minor <= 0);
    }

    /**
     * Validate the given {@link GLData} and throw an exception on validation error.
     * 
     * @param attribs
     *            the {@link GLData} to validate
     */
    static void validateAttributes(GLData attribs) {
        if (attribs.alphaSize < 0) {
            throw new IllegalArgumentException("Alpha bits cannot be less than 0");
        }
        if (attribs.redSize < 0) {
            throw new IllegalArgumentException("Red bits cannot be less than 0");
        }
        if (attribs.greenSize < 0) {
            throw new IllegalArgumentException("Green bits cannot be less than 0");
        }
        if (attribs.blueSize < 0) {
            throw new IllegalArgumentException("Blue bits cannot be less than 0");
        }
        if (attribs.stencilSize < 0) {
            throw new IllegalArgumentException("Stencil bits cannot be less than 0");
        }
        if (attribs.depthSize < 0) {
            throw new IllegalArgumentException("Depth bits cannot be less than 0");
        }
        if (attribs.forwardCompatible && !atLeast30(attribs.majorVersion, attribs.minorVersion)) {
            throw new IllegalArgumentException("Forward-compatibility is only defined for OpenGL version 3.0 and above");
        }
        if (attribs.samples < 0) {
            throw new IllegalArgumentException("Invalid samples count");
        }
        if (attribs.profile != null && !atLeast32(attribs.majorVersion, attribs.minorVersion)) {
            throw new IllegalArgumentException("Context profiles are only defined for OpenGL version 3.2 and above");
        }
        if (attribs.api == null) {
            throw new IllegalArgumentException("Unspecified client API");
        }
        if (attribs.api == API.GL && !validVersionGL(attribs.majorVersion, attribs.minorVersion)) {
            throw new IllegalArgumentException("Invalid OpenGL version");
        }
        if (attribs.api == API.GLES && !validVersionGLES(attribs.majorVersion, attribs.minorVersion)) {
            throw new IllegalArgumentException("Invalid OpenGL ES version");
        }
        if (!attribs.doubleBuffer && attribs.swapInterval != null) {
            throw new IllegalArgumentException("Swap interval set but not using double buffering");
        }
        if (attribs.colorSamplesNV < 0) {
            throw new IllegalArgumentException("Invalid color samples count");
        }
        if (attribs.colorSamplesNV > attribs.samples) {
            throw new IllegalArgumentException("Color samples greater than number of (coverage) samples");
        }
        if (attribs.swapGroupNV < 0) {
            throw new IllegalArgumentException("Invalid swap group");
        }
        if (attribs.swapBarrierNV < 0) {
            throw new IllegalArgumentException("Invalid swap barrier");
        }
        if ((attribs.swapGroupNV > 0 || attribs.swapBarrierNV > 0) && !attribs.doubleBuffer) {
            throw new IllegalArgumentException("Swap group or barrier requested but not using double buffering");
        }
        if (attribs.swapBarrierNV > 0 && attribs.swapGroupNV == 0) {
            throw new IllegalArgumentException("Swap barrier requested but no valid swap group set");
        }
        if (attribs.loseContextOnReset && !attribs.robustness) {
            throw new IllegalArgumentException("Lose context notification requested but not using robustness");
        }
        if (attribs.contextResetIsolation && !attribs.robustness) {
            throw new IllegalArgumentException("Context reset isolation requested but not using robustness");
        }
    }

}
