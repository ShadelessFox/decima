package com.shade.decima.app.viewport;

import org.lwjgl.opengl.GLDebugMessageCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.opengl.GL43.*;

final class ViewportDebugCallback extends GLDebugMessageCallback {
    private static final Logger log = LoggerFactory.getLogger(ViewportDebugCallback.class);

    @Override
    public void invoke(int source, int type, int id, int severity, int length, long message, long userParam) {
        var sourceString = switch (source) {
            case GL_DEBUG_SOURCE_API -> "API";
            case GL_DEBUG_SOURCE_WINDOW_SYSTEM -> "Window System";
            case GL_DEBUG_SOURCE_SHADER_COMPILER -> "Shader Compiler";
            case GL_DEBUG_SOURCE_THIRD_PARTY -> "Third-Party";
            case GL_DEBUG_SOURCE_APPLICATION -> "Application";
            case GL_DEBUG_SOURCE_OTHER -> "Other";
            default -> throw new IllegalStateException("Unexpected source value: " + source);
        };

        var typeString = switch (type) {
            case GL_DEBUG_TYPE_ERROR -> "Error";
            case GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR -> "Deprecated Behavior";
            case GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR -> "Undefined Behavior";
            case GL_DEBUG_TYPE_PORTABILITY -> "Portability";
            case GL_DEBUG_TYPE_PERFORMANCE -> "Performance";
            case GL_DEBUG_TYPE_MARKER -> "Marker";
            case GL_DEBUG_TYPE_PUSH_GROUP -> "Push Group";
            case GL_DEBUG_TYPE_POP_GROUP -> "Pop Group";
            case GL_DEBUG_TYPE_OTHER -> "Other";
            default -> throw new IllegalStateException("Unexpected type value: " + type);
        };

        var severityString = switch (severity) {
            case GL_DEBUG_SEVERITY_LOW -> "Low"; // info
            case GL_DEBUG_SEVERITY_MEDIUM -> "Medium"; // warning
            case GL_DEBUG_SEVERITY_HIGH -> "High"; // error
            case GL_DEBUG_SEVERITY_NOTIFICATION -> "Notification"; // trace
            default -> throw new IllegalStateException("Unexpected severity value: " + severity);
        };

        if (severity == GL_DEBUG_SEVERITY_NOTIFICATION) {
            log.debug("[source: {}, type: {}, severity: {}] {}", sourceString, typeString, severityString, getMessage(length, message));
        } else {
            log.info("[source: {}, type: {}, severity: {}] {}", sourceString, typeString, severityString, getMessage(length, message));
        }
    }
}
