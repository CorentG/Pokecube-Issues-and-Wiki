package thut.core.client.render.wrappers;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3f;
import thut.api.ModelHolder;
import thut.api.entity.IMobColourable;
import thut.api.maths.Vector3;
import thut.api.maths.Vector4;
import thut.core.client.render.animation.Animation;
import thut.core.client.render.animation.AnimationHelper;
import thut.core.client.render.animation.AnimationXML.Mat;
import thut.core.client.render.animation.IAnimationChanger;
import thut.core.client.render.model.IExtendedModelPart;
import thut.core.client.render.model.IModel;
import thut.core.client.render.model.IModelRenderer;
import thut.core.client.render.model.IModelRenderer.Vector5;
import thut.core.client.render.model.ModelFactory;
import thut.core.client.render.texturing.IRetexturableModel;
import thut.core.common.mobs.DefaultColourable;

public class ModelWrapper<T extends Entity> extends EntityModel<T> implements IModel
{
    private static final HeadInfo DUMMY = new HeadInfo();

    public final ModelHolder       model;
    public final IModelRenderer<?> renderer;
    public IModel                  imodel;
    private T                      entityIn;
    protected float                rotationPointX = 0, rotationPointY = 0, rotationPointZ = 0;
    protected float                rotateAngleX   = 0, rotateAngleY = 0, rotateAngleZ = 0, rotateAngle = 0;

    private final int[] tmp = new int[4];

    private final Vector5 rots = new Vector5();

    public ModelWrapper(final ModelHolder model, final IModelRenderer<?> renderer)
    {
        this.model = model;
        this.renderer = renderer;
    }

    public void SetEntity(final T entity)
    {
        this.entityIn = entity;
    }

    @Override
    public void applyAnimation(final Entity entity, final IModelRenderer<?> renderer, final float partialTicks,
            final float limbSwing)
    {
        if (!this.isLoaded()) return;
        this.imodel.applyAnimation(entity, renderer, partialTicks, limbSwing);
    }

    @Override
    public HeadInfo getHeadInfo()
    {
        if (!this.isLoaded()) return ModelWrapper.DUMMY;
        return this.imodel.getHeadInfo();
    }

    @Override
    public Set<String> getHeadParts()
    {
        if (!this.isLoaded()) return Collections.emptySet();
        return this.imodel.getHeadParts();
    }

    @Override
    public Map<String, IExtendedModelPart> getParts()
    {
        if (!this.isLoaded()) return Collections.emptyMap();
        return this.imodel.getParts();
    }

    @Override
    public boolean isValid()
    {
        // Wait for the imodel before claiming to be invalid
        if (this.imodel == null) return true;
        return this.imodel.isValid();
    }

    @Override
    public boolean isLoaded()
    {
        // If we have no model, obviously not loaded yet
        if (this.imodel == null) return false;
        // Otherwise ask the model
        return this.imodel.isLoaded();
    }

    @Override
    public void preProcessAnimations(final Collection<List<Animation>> collection)
    {
        if (!this.isLoaded()) return;
        this.imodel.preProcessAnimations(collection);
    }

    private void initColours(final IExtendedModelPart parent, final T entity, final int brightness, final int overlay)
    {
        int red = 255, green = 255, blue = 255;
        int alpha = 255;
        final IMobColourable poke = entity.getCapability(DefaultColourable.CAPABILITY).orElse(null);

        if (poke != null)
        {
            red = poke.getRGBA()[0];
            green = poke.getRGBA()[1];
            blue = poke.getRGBA()[2];
            alpha = poke.getRGBA()[3];
        }
        final IAnimationChanger animChanger = this.renderer.getAnimationChanger();
        if (animChanger != null && animChanger.modifyColourForPart(parent.getName(), entity, this.tmp))
        {
            red = this.tmp[0];
            green = this.tmp[1];
            blue = this.tmp[2];
            alpha = this.tmp[3];
        }
        parent.setRGBABrO(red, green, blue, alpha, brightness, overlay);
        for (final String partName : parent.getSubParts().keySet())
        {
            final IExtendedModelPart part = parent.getSubParts().get(partName);
            this.initColours(part, entity, brightness, overlay);
        }
    }

    @Override
    public void setRotationAngles(final T entityIn, final float limbSwing, final float limbSwingAmount,
            final float ageInTicks, final float netHeadYaw, final float headPitch)
    {
        if (this.imodel == null) this.imodel = ModelFactory.create(this.model);
        if (!this.isLoaded()) return;
        this.entityIn = entityIn;
        final HeadInfo info = this.imodel.getHeadInfo();
        if (info != null)
        {
            info.headPitch = headPitch;
            info.headYaw = netHeadYaw;
        }
        if (info != null) info.currentTick = entityIn.ticksExisted;
        final IAnimationChanger animChanger = this.renderer.getAnimationChanger();
        final Set<String> excluded = Sets.newHashSet();
        if (animChanger != null) for (final String partName : this.imodel.getParts().keySet())
        {
            if (animChanger.isPartHidden(partName, entityIn, false)) excluded.add(partName);
            if (this.renderer.getTexturer() != null) this.renderer.getTexturer().bindObject(entityIn);
        }
        if (info != null) info.lastTick = entityIn.ticksExisted;
    }

    @Override
    public void render(final MatrixStack mat, final IVertexBuilder buffer, final int packedLightIn,
            final int packedOverlayIn, final float red, final float green, final float blue, final float alpha)
    {
        if (this.imodel == null) this.imodel = ModelFactory.create(this.model);
        if (!this.isLoaded()) return;
        mat.push();
        this.transformGlobal(mat, buffer, this.renderer.getAnimation(this.entityIn), this.entityIn, Minecraft
                .getInstance().getRenderPartialTicks());

        final IAnimationChanger animChanger = this.renderer.getAnimationChanger();
        final Set<String> excluded = Sets.newHashSet();
        if (animChanger != null) for (final String partName : this.imodel.getParts().keySet())
        {
            if (animChanger.isPartHidden(partName, this.entityIn, false)) excluded.add(partName);
            if (this.renderer.getTexturer() != null) this.renderer.getTexturer().bindObject(this.entityIn);
        }

        for (final String partName : this.imodel.getParts().keySet())
        {
            final IExtendedModelPart part = this.imodel.getParts().get(partName);
            if (part == null) continue;
            try
            {
                if (part instanceof IRetexturableModel) ((IRetexturableModel) part).setTexturer(this.renderer
                        .getTexturer());

                if (part.getParent() == null)
                {
                    mat.push();
                    this.initColours(part, this.entityIn, packedLightIn, packedOverlayIn);
                    part.renderAllExcept(mat, buffer, this.renderer, excluded.toArray(new String[excluded.size()]));
                    mat.pop();
                }
            }
            catch (final Exception e)
            {
                e.printStackTrace();
            }
        }
        mat.pop();
    }

    protected void rotate(final MatrixStack mat)
    {
        final Vector3f axis = new Vector3f(this.rotateAngleX, this.rotateAngleY, this.rotateAngleZ);
        mat.rotate(new Quaternion(axis, this.rotateAngle, true));
    }

    /**
     * setLivingAnimations <br>
     * <br>
     * Used for easily adding entity-dependent animations. The second and third
     * float params here are the same second and third as in the
     * setRotationAngles method.
     */
    @Override
    public void setLivingAnimations(final T entityIn, final float limbSwing, final float limbSwingAmount,
            final float partialTickTime)
    {
        if (this.imodel == null) this.imodel = ModelFactory.create(this.model);
        if (!this.isLoaded()) return;
        this.renderer.setAnimationHolder(AnimationHelper.getHolder(entityIn));
        if (this.renderer.getAnimationChanger() != null) this.renderer.setAnimation(entityIn, partialTickTime);
        this.applyAnimation(entityIn, this.renderer, partialTickTime, limbSwing);
    }

    @Override
    public void setOffset(final Vector3 point)
    {
        this.setRotationPoint((float) point.x, (float) point.y, (float) point.z);
    }

    public void setRotationAngles(final Vector4 rotations)
    {
        this.rotateAngle = rotations.w;
        this.rotateAngleX = rotations.x;
        this.rotateAngleY = rotations.y;
        this.rotateAngleZ = rotations.z;
    }

    public void setRotationPoint(final float par1, final float par2, final float par3)
    {
        this.rotationPointX = par1;
        this.rotationPointY = par2;
        this.rotationPointZ = par3;
    }

    protected void transformGlobal(final MatrixStack mat, final IVertexBuilder buffer, final String currentPhase,
            final Entity entity, final float partialTick)
    {
        Vector5 rotations = this.renderer.getRotations();
        if (rotations == null) rotations = this.rots;
        this.setRotationAngles(rotations.rotations);
        this.setOffset(this.renderer.getRotationOffset());
        this.rotate(mat);
        this.imodel.globalFix(mat, this.rotationPointX, this.rotationPointY, this.rotationPointZ);
        this.translate(mat);
        this.renderer.scaleEntity(mat, entity, this, partialTick);
    }

    private void translate(final MatrixStack mat)
    {
        mat.translate(this.rotationPointX, this.rotationPointY, this.rotationPointZ);
    }

    @Override
    public void updateMaterial(final Mat mat)
    {
        this.imodel.updateMaterial(mat);
        IModel.super.updateMaterial(mat);
    }

}
