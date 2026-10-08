package org.fentanylsolutions.thaumicdabblery.feature.effigyskins;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

import org.lwjgl.opengl.GL11;

/** 64x64 skins use independent left limbs and clothing layers. Retains the stock effigy's pose and scale. */
final class EffigySkinModel extends ModelBiped {

    private final ModelRenderer jacket, rightSleeve, leftSleeve, rightTrouser, leftTrouser;

    EffigySkinModel(boolean modern, boolean slim) {
        super(0, 0, 64, modern ? 64 : 32);
        if (!modern) {
            jacket = rightSleeve = leftSleeve = rightTrouser = leftTrouser = null;
            return;
        }
        int armWidth = slim ? 3 : 4;
        bipedRightArm = part(40, 16, -(armWidth - 1), -2, -2, armWidth, 12, 4, 0, -5, slim ? 2.5F : 2, 0);
        bipedLeftArm = part(32, 48, -1, -2, -2, armWidth, 12, 4, 0, 5, slim ? 2.5F : 2, 0);
        bipedLeftLeg = part(16, 48, -2, 0, -2, 4, 12, 4, 0, 1.9F, 12, 0);
        jacket = part(16, 32, -4, 0, -2, 8, 12, 4, 0.25F, 0, 0, 0);
        rightSleeve = part(40, 32, -(armWidth - 1), -2, -2, armWidth, 12, 4, 0.25F, -5, slim ? 2.5F : 2, 0);
        leftSleeve = part(48, 48, -1, -2, -2, armWidth, 12, 4, 0.25F, 5, slim ? 2.5F : 2, 0);
        rightTrouser = part(0, 32, -2, 0, -2, 4, 12, 4, 0.25F, -1.9F, 12, 0);
        leftTrouser = part(0, 48, -2, 0, -2, 4, 12, 4, 0.25F, 1.9F, 12, 0);
    }

    private ModelRenderer part(int u, int v, float x, float y, float z, int w, int h, int d, float inflation, float px,
        float py, float pz) {
        ModelRenderer part = new ModelRenderer(this, u, v);
        part.addBox(x, y, z, w, h, d, inflation);
        part.setRotationPoint(px, py, pz);
        return part;
    }

    private void layer(ModelRenderer layer, ModelRenderer base, float scale) {
        layer.rotateAngleX = base.rotateAngleX;
        layer.rotateAngleY = base.rotateAngleY;
        layer.rotateAngleZ = base.rotateAngleZ;
        layer.render(scale);
    }

    @Override
    public void render(Entity entity, float limb, float amount, float age, float yaw, float pitch, float scale) {
        setRotationAngles(limb, amount, age, yaw, pitch, scale, entity);
        // Horizons renders its effigy with ModelBiped's default child proportions.
        // Keep those proportions, applying the same transform to each part's clothing.
        GL11.glPushMatrix();
        GL11.glScalef(0.75F, 0.75F, 0.75F);
        GL11.glTranslatef(0, 16 * scale, 0);
        bipedHead.render(scale);
        bipedHeadwear.render(scale);
        GL11.glPopMatrix();
        GL11.glPushMatrix();
        GL11.glScalef(0.5F, 0.5F, 0.5F);
        GL11.glTranslatef(0, 24 * scale, 0);
        bipedBody.render(scale);
        bipedRightArm.render(scale);
        bipedLeftArm.render(scale);
        bipedRightLeg.render(scale);
        bipedLeftLeg.render(scale);
        if (jacket != null) {
            layer(jacket, bipedBody, scale);
            layer(rightSleeve, bipedRightArm, scale);
            layer(leftSleeve, bipedLeftArm, scale);
            layer(rightTrouser, bipedRightLeg, scale);
            layer(leftTrouser, bipedLeftLeg, scale);
        }
        GL11.glPopMatrix();
    }
}
