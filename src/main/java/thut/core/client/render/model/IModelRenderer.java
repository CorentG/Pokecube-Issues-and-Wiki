package thut.core.client.render.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nullable;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import thut.api.ModelHolder;
import thut.api.maths.Vector3;
import thut.api.maths.Vector4;
import thut.core.client.render.animation.Animation;
import thut.core.client.render.animation.CapabilityAnimation.IAnimationHolder;
import thut.core.client.render.animation.IAnimationChanger;
import thut.core.client.render.texturing.IPartTexturer;

public interface IModelRenderer<T extends MobEntity>
{
    public static class Vector5
    {
        public Vector4 rotations;
        public int     time;

        public Vector5()
        {
            this.time = 0;
            this.rotations = new Vector4();
        }

        public Vector5(final Vector4 rotation, final int time)
        {
            this.rotations = rotation;
            this.time = time;
        }

        @Override
        public String toString()
        {
            return "|r:" + this.rotations + "|t:" + this.time;
        }
    }

    public static final String DEFAULTPHASE = "idle";

    static final Vector3       DEFAULTSCALE = Vector3.getNewVector().set(1);

    default void doRender(T entity, double d, double d1, double d2, float f, float partialTick)
    {

    }

    default String getAnimation(final Entity entityIn)
    {
        final IAnimationHolder holder = this.getAnimationHolder();
        if (holder != null) return holder.getAnimation(entityIn);
        return IModelRenderer.DEFAULTPHASE;
    }

    IAnimationChanger getAnimationChanger();

    HashMap<String, List<Animation>> getAnimations();

    default Vector3 getRotationOffset()
    {
        return Vector3.empty;
    }

    @Nullable
    default Vector5 getRotations()
    {
        return null;
    }

    default Vector3 getScale()
    {
        return IModelRenderer.DEFAULTSCALE;
    }

    IPartTexturer getTexturer();

    boolean hasAnimation(String phase, Entity entity);

    void scaleEntity(MatrixStack mat, Entity entity, IModel model, float partialTick);

    default void setAnimation(final Entity entity, final float partialTick)
    {
        final IAnimationHolder holder = this.getAnimationHolder();
        final String phase = this.getAnimation(entity);
        final List<Animation> anim = this.getAnimations(entity, phase);
        if (holder != null && anim != null && !anim.isEmpty())
        {
            phase.toString();
            // System.out.println(anim + " " + holder.getPendingAnimations() + "
            // " + holder.getPlaying().size());
            // System.out.println(holder.getPlaying());
            holder.setPendingAnimations(anim, phase);
        }
    }

    default List<Animation> getAnimations(final Entity entity, final String phase)
    {
        return null;
    }

    void setAnimationHolder(IAnimationHolder holder);

    IAnimationHolder getAnimationHolder();

    void setAnimationChanger(IAnimationChanger changer);

    void setRotationOffset(Vector3 offset);

    void setRotations(Vector5 rotations);

    void setScale(Vector3 scale);

    void setTexturer(IPartTexturer texturer);

    void updateModel(HashMap<String, ArrayList<Vector5>> phaseList, ModelHolder model);
}
