package com.shade.platform.ui.editors.spi;

import com.shade.util.NotNull;

public interface EditorOnboardingProvider {
    @NotNull
    Iterable<EditorOnboarding> getOnboardings();
}
