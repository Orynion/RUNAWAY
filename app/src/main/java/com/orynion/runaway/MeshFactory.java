package com.orynion.runaway;

import java.util.ArrayList;

/**
 * Procedural 3D mesh generator for all game objects.
 * Creates geometry with positions, normals, and vertex colors.
 */
public class MeshFactory {

    /**
     * Helper builder for assembling complex composite meshes.
     */
    public static class Builder {
        private final ArrayList<Float> vertices = new ArrayList<>();
        private final ArrayList<Short> indices = new ArrayList<>();
        private short currentIndex = 0;

        public void addVertex(float x, float y, float z, float nx, float ny, float nz, float r, float g, float b, float a) {
            vertices.add(x);
            vertices.add(y);
            vertices.add(z);
            vertices.add(nx);
            vertices.add(ny);
            vertices.add(nz);
            vertices.add(r);
            vertices.add(g);
            vertices.add(b);
            vertices.add(a);
        }

        public void addQuad(float x1, float y1, float z1,
                            float x2, float y2, float z2,
                            float x3, float y3, float z3,
                            float x4, float y4, float z4,
                            float nx, float ny, float nz,
                            float r, float g, float b, float a) {
            short i0 = currentIndex++;
            short i1 = currentIndex++;
            short i2 = currentIndex++;
            short i3 = currentIndex++;

            addVertex(x1, y1, z1, nx, ny, nz, r, g, b, a);
            addVertex(x2, y2, z2, nx, ny, nz, r, g, b, a);
            addVertex(x3, y3, z3, nx, ny, nz, r, g, b, a);
            addVertex(x4, y4, z4, nx, ny, nz, r, g, b, a);

            // Triangle 1: 0, 1, 2
            indices.add(i0);
            indices.add(i1);
            indices.add(i2);
            // Triangle 2: 0, 2, 3
            indices.add(i0);
            indices.add(i2);
            indices.add(i3);
        }

        public void addBox(float cx, float cy, float cz, float w, float h, float d, float r, float g, float b, float a) {
            float hw = w * 0.5f;
            float hh = h * 0.5f;
            float hd = d * 0.5f;

            // Front (+Z)
            addQuad(cx - hw, cy - hh, cz + hd,
                    cx + hw, cy - hh, cz + hd,
                    cx + hw, cy + hh, cz + hd,
                    cx - hw, cy + hh, cz + hd,
                    0, 0, 1, r, g, b, a);

            // Back (-Z)
            addQuad(cx + hw, cy - hh, cz - hd,
                    cx - hw, cy - hh, cz - hd,
                    cx - hw, cy + hh, cz - hd,
                    cx + hw, cy + hh, cz - hd,
                    0, 0, -1, r * 0.85f, g * 0.85f, b * 0.85f, a);

            // Top (+Y)
            addQuad(cx - hw, cy + hh, cz + hd,
                    cx + hw, cy + hh, cz + hd,
                    cx + hw, cy + hh, cz - hd,
                    cx - hw, cy + hh, cz - hd,
                    0, 1, 0, Math.min(1.0f, r * 1.15f), Math.min(1.0f, g * 1.15f), Math.min(1.0f, b * 1.15f), a);

            // Bottom (-Y)
            addQuad(cx - hw, cy - hh, cz - hd,
                    cx + hw, cy - hh, cz - hd,
                    cx + hw, cy - hh, cz + hd,
                    cx - hw, cy - hh, cz + hd,
                    0, -1, 0, r * 0.6f, g * 0.6f, b * 0.6f, a);

            // Right (+X)
            addQuad(cx + hw, cy - hh, cz + hd,
                    cx + hw, cy - hh, cz - hd,
                    cx + hw, cy + hh, cz - hd,
                    cx + hw, cy + hh, cz + hd,
                    1, 0, 0, r * 0.95f, g * 0.95f, b * 0.95f, a);

            // Left (-X)
            addQuad(cx - hw, cy - hh, cz - hd,
                    cx - hw, cy - hh, cz + hd,
                    cx - hw, cy + hh, cz + hd,
                    cx - hw, cy + hh, cz - hd,
                    -1, 0, 0, r * 0.8f, g * 0.8f, b * 0.8f, a);
        }

        public void addCylinder(float cx, float cy, float cz, float radius, float height, int slices, float r, float g, float b, float a) {
            float hh = height * 0.5f;
            float angleStep = (float) (2.0 * Math.PI / slices);

            for (int i = 0; i < slices; i++) {
                float a1 = i * angleStep;
                float a2 = (i + 1) * angleStep;

                float cos1 = (float) Math.cos(a1);
                float sin1 = (float) Math.sin(a1);
                float cos2 = (float) Math.cos(a2);
                float sin2 = (float) Math.sin(a2);

                float x1 = cx + radius * cos1;
                float z1 = cz + radius * sin1;
                float x2 = cx + radius * cos2;
                float z2 = cz + radius * sin2;

                // Side quad
                float nx = (cos1 + cos2) * 0.5f;
                float nz = (sin1 + sin2) * 0.5f;

                addQuad(x1, cy - hh, z1,
                        x2, cy - hh, z2,
                        x2, cy + hh, z2,
                        x1, cy + hh, z1,
                        nx, 0, nz, r, g, b, a);

                // Top cap
                short topCenter = currentIndex++;
                short top1 = currentIndex++;
                short top2 = currentIndex++;
                addVertex(cx, cy + hh, cz, 0, 1, 0, r * 1.1f, g * 1.1f, b * 1.1f, a);
                addVertex(x1, cy + hh, z1, 0, 1, 0, r * 1.1f, g * 1.1f, b * 1.1f, a);
                addVertex(x2, cy + hh, z2, 0, 1, 0, r * 1.1f, g * 1.1f, b * 1.1f, a);
                indices.add(topCenter);
                indices.add(top1);
                indices.add(top2);
            }
        }

        public Mesh build() {
            float[] vertArray = new float[vertices.size()];
            for (int i = 0; i < vertices.size(); i++) {
                vertArray[i] = vertices.get(i);
            }

            short[] indArray = new short[indices.size()];
            for (int i = 0; i < indices.size(); i++) {
                indArray[i] = indices.get(i);
            }

            return new Mesh(vertArray, indArray);
        }
    }

    public static Mesh createBox(float w, float h, float d, float r, float g, float b, float a) {
        Builder builder = new Builder();
        builder.addBox(0, 0, 0, w, h, d, r, g, b, a);
        return builder.build();
    }

    /**
     * Billboard / Particle quad centered at origin.
     */
    public static Mesh createQuad(float w, float h, float r, float g, float b, float a) {
        Builder bld = new Builder();
        float hw = w * 0.5f;
        float hh = h * 0.5f;
        bld.addQuad(-hw, -hh, 0,
                     hw, -hh, 0,
                     hw,  hh, 0,
                    -hw,  hh, 0,
                    0, 0, 1, r, g, b, a);
        return bld.build();
    }

    /**
     * Desert ground tile.
     */
    public static Mesh createGroundTile(float width, float length) {
        Builder bld = new Builder();
        float hw = width * 0.5f;
        float hl = length * 0.5f;

        // Sand terrain top surface
        bld.addQuad(-hw, 0,  hl,
                     hw, 0,  hl,
                     hw, 0, -hl,
                    -hw, 0, -hl,
                    0, 1, 0, 0.86f, 0.70f, 0.44f, 1.0f);
        return bld.build();
    }

    /**
     * Railway track segment containing ballast gravel bed, wooden sleepers, and twin steel rails.
     */
    public static Mesh createTrackSegment(float segmentLength) {
        Builder bld = new Builder();

        // 1. Gravel ballast bed underneath track
        bld.addBox(0, 0.05f, 0, 3.4f, 0.12f, segmentLength, 0.42f, 0.38f, 0.34f, 1.0f);

        // 2. Wooden sleepers (cross ties) spaced every 0.85m
        float sleeperSpacing = 0.85f;
        int numSleepers = (int) (segmentLength / sleeperSpacing);
        float startZ = -segmentLength * 0.5f + (sleeperSpacing * 0.5f);

        for (int i = 0; i < numSleepers; i++) {
            float sz = startZ + i * sleeperSpacing;
            // Alternating subtle wood tone variations
            float woodVar = ((i % 3) == 0) ? 0.04f : 0.0f;
            bld.addBox(0, 0.14f, sz, 2.6f, 0.10f, 0.32f, 0.28f + woodVar, 0.20f + woodVar, 0.14f, 1.0f);

            // Metal tie plates on each side
            bld.addBox(-0.75f, 0.19f, sz, 0.24f, 0.02f, 0.28f, 0.25f, 0.25f, 0.26f, 1.0f);
            bld.addBox( 0.75f, 0.19f, sz, 0.24f, 0.02f, 0.28f, 0.25f, 0.25f, 0.26f, 1.0f);
        }

        // 3. Steel Rails (Left & Right)
        float railGauge = 0.75f; // half gauge = 0.75m -> total 1.5m standard gauge
        // Rail base
        bld.addBox(-railGauge, 0.21f, 0, 0.14f, 0.04f, segmentLength, 0.50f, 0.52f, 0.55f, 1.0f);
        bld.addBox( railGauge, 0.21f, 0, 0.14f, 0.04f, segmentLength, 0.50f, 0.52f, 0.55f, 1.0f);
        // Rail stem
        bld.addBox(-railGauge, 0.26f, 0, 0.05f, 0.08f, segmentLength, 0.45f, 0.47f, 0.50f, 1.0f);
        bld.addBox( railGauge, 0.26f, 0, 0.05f, 0.08f, segmentLength, 0.45f, 0.47f, 0.50f, 1.0f);
        // Rail top head (shiny steel)
        bld.addBox(-railGauge, 0.31f, 0, 0.10f, 0.04f, segmentLength, 0.85f, 0.88f, 0.92f, 1.0f);
        bld.addBox( railGauge, 0.31f, 0, 0.10f, 0.04f, segmentLength, 0.85f, 0.88f, 0.92f, 1.0f);

        return bld.build();
    }

    /**
     * Train flatbed carriage where the player stands.
     */
    public static Mesh createTrainCarriage() {
        Builder bld = new Builder();

        float length = 9.0f;
        float width = 3.2f;

        // Steel frame chassis
        bld.addBox(0, 0.45f, 0, width, 0.25f, length, 0.22f, 0.22f, 0.24f, 1.0f);

        // Wooden deck planks
        bld.addBox(0, 0.60f, 0, width - 0.1f, 0.08f, length - 0.2f, 0.44f, 0.29f, 0.18f, 1.0f);

        // Side safety guardrails (stanchions + top rail)
        float railHeight = 1.1f;
        float hw = width * 0.5f - 0.1f;
        float hl = length * 0.5f - 0.2f;

        // Corner and intermediate posts
        int numPosts = 6;
        for (int i = 0; i < numPosts; i++) {
            float pz = -hl + (i * (2.0f * hl / (numPosts - 1)));
            // Left post
            bld.addBox(-hw, 0.60f + railHeight * 0.5f, pz, 0.08f, railHeight, 0.08f, 0.65f, 0.60f, 0.25f, 1.0f);
            // Right post
            bld.addBox( hw, 0.60f + railHeight * 0.5f, pz, 0.08f, railHeight, 0.08f, 0.65f, 0.60f, 0.25f, 1.0f);
        }

        // Top horizontal guardrails
        bld.addBox(-hw, 0.60f + railHeight, 0, 0.08f, 0.06f, length - 0.4f, 0.72f, 0.68f, 0.28f, 1.0f);
        bld.addBox( hw, 0.60f + railHeight, 0, 0.08f, 0.06f, length - 0.4f, 0.72f, 0.68f, 0.28f, 1.0f);

        // Rear bulkhead & steel plate
        bld.addBox(0, 0.60f + railHeight * 0.5f, -hl, width - 0.2f, railHeight, 0.12f, 0.35f, 0.18f, 0.16f, 1.0f);

        // Forward brake wheel stand & lantern
        bld.addBox(-hw + 0.35f, 1.1f, hl - 0.4f, 0.08f, 0.9f, 0.08f, 0.20f, 0.20f, 0.22f, 1.0f);
        bld.addBox(-hw + 0.35f, 1.55f, hl - 0.4f, 0.35f, 0.05f, 0.35f, 0.75f, 0.30f, 0.20f, 1.0f);

        // Wheel bogies (trucks) underneath
        bld.addBox(-1.0f, 0.25f, -2.5f, 0.15f, 0.35f, 0.8f, 0.18f, 0.18f, 0.20f, 1.0f);
        bld.addBox( 1.0f, 0.25f, -2.5f, 0.15f, 0.35f, 0.8f, 0.18f, 0.18f, 0.20f, 1.0f);
        bld.addBox(-1.0f, 0.25f,  2.5f, 0.15f, 0.35f, 0.8f, 0.18f, 0.18f, 0.20f, 1.0f);
        bld.addBox( 1.0f, 0.25f,  2.5f, 0.15f, 0.35f, 0.8f, 0.18f, 0.18f, 0.20f, 1.0f);

        return bld.build();
    }

    /**
     * Desert telegraph pole with crossarm and ceramic insulators.
     */
    public static Mesh createTelegraphPole() {
        Builder bld = new Builder();

        // Main wooden pole (height 6.5m)
        bld.addBox(0, 3.25f, 0, 0.24f, 6.5f, 0.24f, 0.38f, 0.26f, 0.16f, 1.0f);

        // Horizontal crossarm
        bld.addBox(0, 5.8f, 0, 2.2f, 0.16f, 0.14f, 0.42f, 0.30f, 0.18f, 1.0f);

        // Crossarm angle support braces
        bld.addBox(-0.45f, 5.45f, 0, 0.08f, 0.55f, 0.08f, 0.25f, 0.25f, 0.26f, 1.0f);
        bld.addBox( 0.45f, 5.45f, 0, 0.08f, 0.55f, 0.08f, 0.25f, 0.25f, 0.26f, 1.0f);

        // Ceramic insulators on crossarm (greenish/white glass)
        float[] pins = {-0.9f, -0.45f, 0.45f, 0.9f};
        for (float px : pins) {
            bld.addBox(px, 6.0f, 0, 0.10f, 0.22f, 0.10f, 0.70f, 0.85f, 0.80f, 1.0f);
        }

        return bld.build();
    }

    /**
     * Distant desert mesa / canyon plateau.
     */
    public static Mesh createMesa(float width, float height, float length) {
        Builder bld = new Builder();

        // Multi-tiered stratified sandstone cliff
        // Base tier
        bld.addBox(0, height * 0.25f, 0, width, height * 0.5f, length, 0.72f, 0.38f, 0.24f, 1.0f);
        // Middle strata tier (slightly recessed, lighter ochre)
        bld.addBox(0, height * 0.65f, 0, width * 0.85f, height * 0.35f, length * 0.85f, 0.82f, 0.48f, 0.28f, 1.0f);
        // Top caprock tier (darker rim)
        bld.addBox(0, height * 0.92f, 0, width * 0.75f, height * 0.20f, length * 0.75f, 0.65f, 0.32f, 0.20f, 1.0f);

        return bld.build();
    }

    /**
     * Desert boulder / rock cluster.
     */
    public static Mesh createRock(float size) {
        Builder bld = new Builder();
        // Central block
        bld.addBox(0, size * 0.45f, 0, size * 0.9f, size * 0.9f, size * 0.85f, 0.62f, 0.50f, 0.38f, 1.0f);
        // Offset side wedge
        bld.addBox(size * 0.35f, size * 0.30f, size * 0.2f, size * 0.7f, size * 0.6f, size * 0.7f, 0.58f, 0.46f, 0.34f, 1.0f);
        // Front ledge
        bld.addBox(-size * 0.25f, size * 0.22f, -size * 0.25f, size * 0.6f, size * 0.45f, size * 0.6f, 0.68f, 0.54f, 0.40f, 1.0f);
        return bld.build();
    }

    /**
     * Saguaro cactus with central trunk and upturned arms.
     */
    public static Mesh createCactus() {
        Builder bld = new Builder();
        float r = 0.26f, g = 0.48f, b = 0.24f; // Dusty sage green

        // Main trunk
        bld.addBox(0, 1.6f, 0, 0.42f, 3.2f, 0.42f, r, g, b, 1.0f);

        // Left branch (horizontal elbow + vertical arm)
        bld.addBox(-0.55f, 1.5f, 0, 0.75f, 0.34f, 0.34f, r * 0.95f, g * 0.95f, b * 0.95f, 1.0f);
        bld.addBox(-0.75f, 2.1f, 0, 0.34f, 1.2f, 0.34f, r * 1.05f, g * 1.05f, b * 1.05f, 1.0f);

        // Right branch (higher elbow)
        bld.addBox(0.55f, 2.0f, 0, 0.75f, 0.34f, 0.34f, r * 0.95f, g * 0.95f, b * 0.95f, 1.0f);
        bld.addBox(0.75f, 2.6f, 0, 0.34f, 1.3f, 0.34f, r * 1.05f, g * 1.05f, b * 1.05f, 1.0f);

        return bld.build();
    }

    /**
     * First-person stylized repeating carbine weapon model.
     */
    public static Mesh createWeapon() {
        Builder bld = new Builder();

        // 1. Receiver housing (gunmetal steel)
        bld.addBox(0, 0, 0, 0.09f, 0.12f, 0.38f, 0.25f, 0.26f, 0.28f, 1.0f);

        // 2. Brass breech plate / side lock
        bld.addBox(0.048f, 0.02f, 0.02f, 0.012f, 0.06f, 0.16f, 0.85f, 0.72f, 0.28f, 1.0f);

        // 3. Octagonal long barrel (forward along -Z)
        bld.addBox(0, 0.03f, -0.42f, 0.055f, 0.055f, 0.52f, 0.20f, 0.21f, 0.22f, 1.0f);

        // 4. Tubular under-barrel magazine
        bld.addBox(0, -0.025f, -0.38f, 0.045f, 0.045f, 0.44f, 0.65f, 0.55f, 0.22f, 1.0f);

        // 5. Wooden forestock grip (under barrel)
        bld.addBox(0, -0.01f, -0.20f, 0.075f, 0.08f, 0.24f, 0.48f, 0.28f, 0.14f, 1.0f);

        // 6. Wooden rear shoulder stock
        bld.addBox(0, -0.04f, 0.32f, 0.07f, 0.14f, 0.30f, 0.44f, 0.25f, 0.12f, 1.0f);
        // Brass buttplate
        bld.addBox(0, -0.04f, 0.48f, 0.075f, 0.15f, 0.02f, 0.85f, 0.72f, 0.28f, 1.0f);

        // 7. Lever loop & trigger guard
        bld.addBox(0, -0.09f, 0.10f, 0.035f, 0.06f, 0.10f, 0.22f, 0.22f, 0.24f, 1.0f);

        // 8. Iron sights: front post and rear notch
        bld.addBox(0, 0.075f, -0.64f, 0.014f, 0.035f, 0.015f, 0.95f, 0.45f, 0.15f, 1.0f); // Front sight blade
        bld.addBox(0, 0.080f, -0.12f, 0.045f, 0.025f, 0.015f, 0.30f, 0.30f, 0.32f, 1.0f); // Rear sight base
        bld.addBox(-0.02f, 0.095f, -0.12f, 0.010f, 0.020f, 0.015f, 0.30f, 0.30f, 0.32f, 1.0f);
        bld.addBox( 0.02f, 0.095f, -0.12f, 0.010f, 0.020f, 0.015f, 0.30f, 0.30f, 0.32f, 1.0f);

        return bld.build();
    }

    /**
     * Scout Drone target model.
     */
    public static Mesh createTargetDrone() {
        Builder bld = new Builder();

        // Main chassis core
        bld.addBox(0, 0, 0, 0.85f, 0.50f, 0.85f, 0.25f, 0.28f, 0.35f, 1.0f);

        // Glowing red optic sensor eye (front)
        bld.addBox(0, 0.02f, 0.44f, 0.42f, 0.18f, 0.05f, 1.0f, 0.15f, 0.10f, 1.0f);

        // Armored head cowling
        bld.addBox(0, 0.30f, 0, 0.65f, 0.18f, 0.70f, 0.85f, 0.45f, 0.15f, 1.0f);

        // Side thruster pods / winglets
        bld.addBox(-0.75f, 0, 0, 0.55f, 0.25f, 0.45f, 0.30f, 0.32f, 0.38f, 1.0f);
        bld.addBox( 0.75f, 0, 0, 0.55f, 0.25f, 0.45f, 0.30f, 0.32f, 0.38f, 1.0f);

        // Thruster exhaust rings (rear)
        bld.addBox(-0.75f, 0, -0.28f, 0.35f, 0.18f, 0.12f, 0.95f, 0.65f, 0.20f, 1.0f);
        bld.addBox( 0.75f, 0, -0.28f, 0.35f, 0.18f, 0.12f, 0.95f, 0.65f, 0.20f, 1.0f);

        // Lower stabilizer fin
        bld.addBox(0, -0.38f, -0.15f, 0.12f, 0.35f, 0.45f, 0.75f, 0.25f, 0.15f, 1.0f);

        return bld.build();
    }

    /**
     * Bandit Outlaw Raider target model.
     */
    public static Mesh createTargetBandit() {
        Builder bld = new Builder();

        // Armored Torso / chestplate
        bld.addBox(0, 0.95f, 0, 0.85f, 0.85f, 0.50f, 0.62f, 0.28f, 0.18f, 1.0f);

        // Belt and holster
        bld.addBox(0, 0.50f, 0, 0.88f, 0.18f, 0.52f, 0.28f, 0.18f, 0.12f, 1.0f);

        // Outlaw robot head
        bld.addBox(0, 1.62f, 0, 0.48f, 0.48f, 0.48f, 0.32f, 0.34f, 0.38f, 1.0f);

        // Visor slit (glowing yellow/orange)
        bld.addBox(0, 1.64f, 0.25f, 0.38f, 0.10f, 0.04f, 1.0f, 0.80f, 0.15f, 1.0f);

        // Wide-brim mechanical hat / brow plate
        bld.addBox(0, 1.90f, 0, 1.15f, 0.10f, 1.15f, 0.22f, 0.16f, 0.12f, 1.0f);
        bld.addBox(0, 2.05f, 0, 0.55f, 0.22f, 0.55f, 0.24f, 0.18f, 0.14f, 1.0f);

        // Shoulder pauldrons
        bld.addBox(-0.62f, 1.30f, 0, 0.32f, 0.35f, 0.44f, 0.78f, 0.42f, 0.15f, 1.0f);
        bld.addBox( 0.62f, 1.30f, 0, 0.32f, 0.35f, 0.44f, 0.78f, 0.42f, 0.15f, 1.0f);

        // Right arm holding blaster aimed forward
        bld.addBox(-0.62f, 0.90f, 0.35f, 0.22f, 0.22f, 0.75f, 0.35f, 0.36f, 0.40f, 1.0f);
        // Blaster in hand
        bld.addBox(-0.62f, 0.95f, 0.80f, 0.16f, 0.26f, 0.40f, 0.20f, 0.20f, 0.22f, 1.0f);
        bld.addBox(-0.62f, 1.02f, 1.05f, 0.08f, 0.08f, 0.28f, 0.85f, 0.65f, 0.25f, 1.0f);

        // Legs / chassis stand
        bld.addBox(-0.25f, -0.15f, 0, 0.28f, 1.15f, 0.30f, 0.24f, 0.25f, 0.28f, 1.0f);
        bld.addBox( 0.25f, -0.15f, 0, 0.28f, 1.15f, 0.30f, 0.24f, 0.25f, 0.28f, 1.0f);

        return bld.build();
    }

    /**
     * Muzzle flash 3D diamond star.
     */
    public static Mesh createMuzzleFlash() {
        Builder bld = new Builder();
        float r = 1.0f, g = 0.90f, b = 0.35f, a = 0.95f;

        // Central diamond
        bld.addBox(0, 0, 0, 0.08f, 0.08f, 0.15f, 1.0f, 1.0f, 0.85f, a);

        // Vertical spike
        bld.addBox(0, 0, 0, 0.025f, 0.28f, 0.08f, r, g, b, a);

        // Horizontal spike
        bld.addBox(0, 0, 0, 0.28f, 0.025f, 0.08f, r, g, b, a);

        return bld.build();
    }
}

