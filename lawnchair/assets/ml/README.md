# AutoCat On-Device ML Model

Place `autocat_categorizer.tflite` here to enable TFLite-based app categorization.

## Model Spec

| Property | Value |
|---|---|
| Input shape  | `[1, 36]` float32 — character-frequency feature vector (26 letters + 10 digits) |
| Output shape | `[1, 14]` float32 — category probability distribution (softmax) |
| Min confidence | 0.50 (results below threshold fall back to keyword classifier) |

## Output Category Order

Index → Category name:
0. Social
1. Entertainment
2. Productivity
3. Games
4. Photography
5. Music
6. News
7. Navigation
8. Finance
9. Health
10. Shopping
11. Education
12. Communication
13. Tools

## Training

Train using app name + package name character frequencies as features.
See `MLCategorizer.buildFeatureVector()` for the exact preprocessing logic.

## Fallback

When no model file is present, `MLCategorizer` automatically uses a keyword-based
classifier that covers hundreds of popular apps and common package name patterns.
