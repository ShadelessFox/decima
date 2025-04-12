package com.shade.decima.geometry;

public sealed interface Semantic {
    Semantic POSITION = new Position();
    Semantic NORMAL = new Normal();
    Semantic TANGENT = new Tangent();
    Semantic TEXTURE_0 = new Texture(0);
    Semantic TEXTURE_1 = new Texture(1);
    Semantic COLOR_0 = new Color(0);
    Semantic COLOR_1 = new Color(1);
    Semantic JOINTS_0 = new Joints(0);
    Semantic JOINTS_1 = new Joints(1);
    Semantic WEIGHTS_0 = new Weights(0);
    Semantic WEIGHTS_1 = new Weights(1);

    record Position() implements Semantic {
    }

    record Normal() implements Semantic {
    }

    record Tangent() implements Semantic {
    }

    record Texture(int n) implements Semantic {
        public Texture {
            if (n < 0) {
                throw new IllegalArgumentException("n must be positive");
            }
        }
    }

    record Color(int n) implements Semantic {
        public Color {
            if (n < 0) {
                throw new IllegalArgumentException("n must be positive");
            }
        }
    }

    record Joints(int n) implements Semantic {
        public Joints {
            if (n < 0) {
                throw new IllegalArgumentException("n must be positive");
            }
        }
    }

    record Weights(int n) implements Semantic {
        public Weights {
            if (n < 0) {
                throw new IllegalArgumentException("n must be positive");
            }
        }
    }
}
