package com.codespace.marvelzombie;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import java.util.List;
import org.joml.Vector3f;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import java.util.UUID;

public class MjolnirItem extends SwordItem {
    private static final DustParticleOptions LIGHTNING_PARTICLE =
            new DustParticleOptions(new Vector3f(0.35F, 0.72F, 1.0F), 1.3F);

    public MjolnirItem(Tier tier, int attackDamage, float attackSpeed, Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("Mjölnir").withStyle(ChatFormatting.YELLOW);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        if (stack.hasTag() && stack.getTag().contains(ThorEnergy.MJOLNIR_OWNER_NAME_TAG)) {
            String ownerName = stack.getTag().getString(ThorEnergy.MJOLNIR_OWNER_NAME_TAG);
            if (!ownerName.isBlank()) {
                tooltip.add(Component.translatable("item.marvel_zombie.mjolnir.owner", ownerName));
            }
        }
        tooltip.add(Component.translatable("item.marvel_zombie.mjolnir.tooltip.charge"));
        tooltip.add(Component.translatable("item.marvel_zombie.mjolnir.tooltip.throw"));
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.MAINHAND) {
            return super.getAttributeModifiers(slot, stack);
        }
        return ImmutableMultimap.<Attribute, AttributeModifier>builder()
                .put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                        UUID.fromString("4e4f6b5d-4f9a-4d33-a8f5-2cb5ce00f001"),
                        "Mjölnir damage", 3.0D, AttributeModifier.Operation.ADDITION))
                .put(Attributes.ATTACK_SPEED, new AttributeModifier(
                        UUID.fromString("4e4f6b5d-4f9a-4d33-a8f5-2cb5ce00f002"),
                        "Mjölnir attack speed", -2.4D, AttributeModifier.Operation.ADDITION))
                .build();
    }

    public static void fireLightningBlast(Level level, LivingEntity source) {
        Vec3 start = source.getEyePosition();
        Vec3 direction = source.getLookAngle();
        Vec3 end = start.add(direction.scale(28.0D));
        HitResult blockHit = level.clip(new net.minecraft.world.level.ClipContext(
                start, end, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, source));
        Vec3 beamEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        AABB search = new AABB(start, beamEnd).inflate(1.0D);
        LivingEntity target = level.getEntitiesOfClass(LivingEntity.class, search,
                        candidate -> candidate != source && candidate.isAlive())
                .stream()
                .filter(candidate -> candidate.getBoundingBox().inflate(0.3D)
                        .clip(start, beamEnd).isPresent())
                .min(java.util.Comparator.comparingDouble(candidate ->
                        candidate.distanceToSqr(source)))
                .orElse(null);

        if (target != null) {
            target.hurt(level.damageSources().indirectMagic(source, source), 14.0F);
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().lightningBolt(), 4.0F);
            target.knockback(0.7D, source.getX() - target.getX(), source.getZ() - target.getZ());
        }

        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            Vec3 beam = beamEnd.subtract(start);
            int steps = Math.max(1, (int) (beam.length() * 2.0D));
            for (int i = 0; i <= steps; i++) {
                Vec3 point = start.add(beam.scale(i / (double) steps));
                serverLevel.sendParticles(LIGHTNING_PARTICLE, point.x, point.y, point.z,
                        2, 0.025D, 0.025D, 0.025D, 0.0D);
                if (i % 5 == 0) {
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            point.x, point.y, point.z, 2, 0.12D, 0.12D, 0.12D, 0.01D);
                }
            }
        }
        level.playSound(null, BlockPos.containing(start), SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.PLAYERS, 1.1F, 1.4F);
    }
}
