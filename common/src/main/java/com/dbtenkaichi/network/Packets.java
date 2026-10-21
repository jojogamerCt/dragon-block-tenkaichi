package com.dbtenkaichi.network;

import com.dbtenkaichi.Tenkaichi;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class Packets {
    public static final int MAX_STRING = 256;
    public static final int MAX_FORMS = 256;
    public static final int MAX_FORM_JSON = 16384;

    private Packets() {
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payloadType(String name) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Tenkaichi.MOD_ID, name));
    }

    public record CreateCharacter(int race, int presentation, float height, int build, int skinColor, int eyeStyle, int eyeColor,
                                  int eyebrowStyle, int hairStyle, int hairColor, int expression, int auraColor,
                                  int outfit, int outfitPrimary, int outfitSecondary, int featureColor, boolean tail, boolean useOwnSkin) implements CustomPacketPayload {
        public static final Type<CreateCharacter> TYPE = payloadType("create_character");
        public static final StreamCodec<RegistryFriendlyByteBuf, CreateCharacter> CODEC = StreamCodec.ofMember(CreateCharacter::write, CreateCharacter::read);

        private static CreateCharacter read(RegistryFriendlyByteBuf buf) {
            return new CreateCharacter(buf.readVarInt(), buf.readVarInt(), buf.readFloat(), buf.readVarInt(), buf.readInt(), buf.readVarInt(), buf.readInt(),
                    buf.readVarInt(), buf.readVarInt(), buf.readInt(), buf.readVarInt(), buf.readInt(),
                    buf.readVarInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readBoolean(), buf.readBoolean());
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(race);
            buf.writeVarInt(presentation);
            buf.writeFloat(height);
            buf.writeVarInt(build);
            buf.writeInt(skinColor);
            buf.writeVarInt(eyeStyle);
            buf.writeInt(eyeColor);
            buf.writeVarInt(eyebrowStyle);
            buf.writeVarInt(hairStyle);
            buf.writeInt(hairColor);
            buf.writeVarInt(expression);
            buf.writeInt(auraColor);
            buf.writeVarInt(outfit);
            buf.writeInt(outfitPrimary);
            buf.writeInt(outfitSecondary);
            buf.writeInt(featureColor);
            buf.writeBoolean(tail);
            buf.writeBoolean(useOwnSkin);
        }

        @Override
        public Type<CreateCharacter> type() {
            return TYPE;
        }
    }

    public record Action(int action, int arg) implements CustomPacketPayload {
        public static final Type<Action> TYPE = payloadType("action");
        public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC = StreamCodec.ofMember(Action::write, Action::read);

        private static Action read(RegistryFriendlyByteBuf buf) {
            return new Action(buf.readVarInt(), buf.readVarInt());
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(action);
            buf.writeVarInt(arg);
        }

        @Override
        public Type<Action> type() {
            return TYPE;
        }
    }

    public record AssignSlot(int slot, String skill) implements CustomPacketPayload {
        public static final Type<AssignSlot> TYPE = payloadType("assign_slot");
        public static final StreamCodec<RegistryFriendlyByteBuf, AssignSlot> CODEC = StreamCodec.ofMember(AssignSlot::write, AssignSlot::read);

        private static AssignSlot read(RegistryFriendlyByteBuf buf) {
            return new AssignSlot(buf.readVarInt(), buf.readUtf(MAX_STRING));
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(slot);
            buf.writeUtf(skill, MAX_STRING);
        }

        @Override
        public Type<AssignSlot> type() {
            return TYPE;
        }
    }

    public record TrainingInput(String activity, int phase, int value) implements CustomPacketPayload {
        public static final Type<TrainingInput> TYPE = payloadType("training_input");
        public static final StreamCodec<RegistryFriendlyByteBuf, TrainingInput> CODEC = StreamCodec.ofMember(TrainingInput::write, TrainingInput::read);

        private static TrainingInput read(RegistryFriendlyByteBuf buf) {
            return new TrainingInput(buf.readUtf(MAX_STRING), buf.readVarInt(), buf.readVarInt());
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeUtf(activity, MAX_STRING);
            buf.writeVarInt(phase);
            buf.writeVarInt(value);
        }

        @Override
        public Type<TrainingInput> type() {
            return TYPE;
        }
    }

    public record SyncData(CompoundTag data) implements CustomPacketPayload {
        public static final Type<SyncData> TYPE = payloadType("sync_data");
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncData> CODEC = StreamCodec.ofMember(SyncData::write, SyncData::read);

        private static SyncData read(RegistryFriendlyByteBuf buf) {
            CompoundTag tag = buf.readNbt();
            return new SyncData(tag == null ? new CompoundTag() : tag);
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeNbt(data);
        }

        @Override
        public Type<SyncData> type() {
            return TYPE;
        }
    }

    public record Vitals(float ki, float maxKi, float stamina, float maxStamina, float fatigue, long battlePower, long trainingPoints, int flags, int chargeTicks)
            implements CustomPacketPayload {
        public static final Type<Vitals> TYPE = payloadType("vitals");
        public static final StreamCodec<RegistryFriendlyByteBuf, Vitals> CODEC = StreamCodec.ofMember(Vitals::write, Vitals::read);

        private static Vitals read(RegistryFriendlyByteBuf buf) {
            return new Vitals(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readVarLong(), buf.readVarLong(), buf.readVarInt(), buf.readVarInt());
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeFloat(ki);
            buf.writeFloat(maxKi);
            buf.writeFloat(stamina);
            buf.writeFloat(maxStamina);
            buf.writeFloat(fatigue);
            buf.writeVarLong(battlePower);
            buf.writeVarLong(trainingPoints);
            buf.writeVarInt(flags);
            buf.writeVarInt(chargeTicks);
        }

        @Override
        public Type<Vitals> type() {
            return TYPE;
        }
    }

    public record Appearance(int entityId, int race, CompoundTag appearance) implements CustomPacketPayload {
        public static final Type<Appearance> TYPE = payloadType("appearance");
        public static final StreamCodec<RegistryFriendlyByteBuf, Appearance> CODEC = StreamCodec.ofMember(Appearance::write, Appearance::read);

        private static Appearance read(RegistryFriendlyByteBuf buf) {
            int id = buf.readVarInt();
            int race = buf.readVarInt();
            CompoundTag tag = buf.readNbt();
            return new Appearance(id, race, tag == null ? new CompoundTag() : tag);
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(entityId);
            buf.writeVarInt(race);
            buf.writeNbt(appearance);
        }

        @Override
        public Type<Appearance> type() {
            return TYPE;
        }
    }

    public record EntityState(int entityId, String form, int flags, float kiRatio, float formMastery) implements CustomPacketPayload {
        public static final Type<EntityState> TYPE = payloadType("entity_state");
        public static final StreamCodec<RegistryFriendlyByteBuf, EntityState> CODEC = StreamCodec.ofMember(EntityState::write, EntityState::read);

        private static EntityState read(RegistryFriendlyByteBuf buf) {
            return new EntityState(buf.readVarInt(), buf.readUtf(MAX_STRING), buf.readVarInt(), buf.readFloat(), buf.readFloat());
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(entityId);
            buf.writeUtf(form, MAX_STRING);
            buf.writeVarInt(flags);
            buf.writeFloat(kiRatio);
            buf.writeFloat(formMastery);
        }

        @Override
        public Type<EntityState> type() {
            return TYPE;
        }
    }

    public record Anim(int entityId, int clip, int durationTicks, int variant) implements CustomPacketPayload {
        public static final Type<Anim> TYPE = payloadType("anim");
        public static final StreamCodec<RegistryFriendlyByteBuf, Anim> CODEC = StreamCodec.ofMember(Anim::write, Anim::read);

        private static Anim read(RegistryFriendlyByteBuf buf) {
            return new Anim(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(entityId);
            buf.writeVarInt(clip);
            buf.writeVarInt(durationTicks);
            buf.writeVarInt(variant);
        }

        @Override
        public Type<Anim> type() {
            return TYPE;
        }
    }

    public record CombatFx(int kind, int attackerId, int targetId, double x, double y, double z, float strength) implements CustomPacketPayload {
        public static final Type<CombatFx> TYPE = payloadType("combat_fx");
        public static final StreamCodec<RegistryFriendlyByteBuf, CombatFx> CODEC = StreamCodec.ofMember(CombatFx::write, CombatFx::read);

        private static CombatFx read(RegistryFriendlyByteBuf buf) {
            return new CombatFx(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat());
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(kind);
            buf.writeVarInt(attackerId);
            buf.writeVarInt(targetId);
            buf.writeDouble(x);
            buf.writeDouble(y);
            buf.writeDouble(z);
            buf.writeFloat(strength);
        }

        @Override
        public Type<CombatFx> type() {
            return TYPE;
        }
    }

    public record Forms(List<String> definitions) implements CustomPacketPayload {
        public static final Type<Forms> TYPE = payloadType("forms");
        public static final StreamCodec<RegistryFriendlyByteBuf, Forms> CODEC = StreamCodec.ofMember(Forms::write, Forms::read);

        private static Forms read(RegistryFriendlyByteBuf buf) {
            int count = Math.min(buf.readVarInt(), MAX_FORMS);
            List<String> list = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                list.add(buf.readUtf(MAX_FORM_JSON));
            }
            return new Forms(list);
        }

        private void write(RegistryFriendlyByteBuf buf) {
            int count = Math.min(definitions.size(), MAX_FORMS);
            buf.writeVarInt(count);
            for (int i = 0; i < count; i++) {
                buf.writeUtf(definitions.get(i), MAX_FORM_JSON);
            }
        }

        @Override
        public Type<Forms> type() {
            return TYPE;
        }
    }

    public record OpenScreen(int screen, CompoundTag data) implements CustomPacketPayload {
        public static final Type<OpenScreen> TYPE = payloadType("open_screen");
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenScreen> CODEC = StreamCodec.ofMember(OpenScreen::write, OpenScreen::read);

        private static OpenScreen read(RegistryFriendlyByteBuf buf) {
            int screen = buf.readVarInt();
            CompoundTag tag = buf.readNbt();
            return new OpenScreen(screen, tag == null ? new CompoundTag() : tag);
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(screen);
            buf.writeNbt(data);
        }

        @Override
        public Type<OpenScreen> type() {
            return TYPE;
        }
    }

    public record Clash(int clashId, int firstId, int secondId, float balance, int phase, int pulseTick, double x, double y, double z) implements CustomPacketPayload {
        public static final Type<Clash> TYPE = payloadType("clash");
        public static final StreamCodec<RegistryFriendlyByteBuf, Clash> CODEC = StreamCodec.ofMember(Clash::write, Clash::read);

        private static Clash read(RegistryFriendlyByteBuf buf) {
            return new Clash(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readFloat(), buf.readVarInt(), buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble());
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(clashId);
            buf.writeVarInt(firstId);
            buf.writeVarInt(secondId);
            buf.writeFloat(balance);
            buf.writeVarInt(phase);
            buf.writeVarInt(pulseTick);
            buf.writeDouble(x);
            buf.writeDouble(y);
            buf.writeDouble(z);
        }

        @Override
        public Type<Clash> type() {
            return TYPE;
        }
    }

    public record Cinematic(int entityId, String form, int ticks, boolean full) implements CustomPacketPayload {
        public static final Type<Cinematic> TYPE = payloadType("cinematic");
        public static final StreamCodec<RegistryFriendlyByteBuf, Cinematic> CODEC = StreamCodec.ofMember(Cinematic::write, Cinematic::read);

        private static Cinematic read(RegistryFriendlyByteBuf buf) {
            return new Cinematic(buf.readVarInt(), buf.readUtf(MAX_STRING), buf.readVarInt(), buf.readBoolean());
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(entityId);
            buf.writeUtf(form, MAX_STRING);
            buf.writeVarInt(ticks);
            buf.writeBoolean(full);
        }

        @Override
        public Type<Cinematic> type() {
            return TYPE;
        }
    }

    public record ServerConfig(String json) implements CustomPacketPayload {
        public static final Type<ServerConfig> TYPE = payloadType("server_config");
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerConfig> CODEC = StreamCodec.ofMember(ServerConfig::write, ServerConfig::read);

        private static ServerConfig read(RegistryFriendlyByteBuf buf) {
            return new ServerConfig(buf.readUtf(262144));
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeUtf(json, 262144);
        }

        @Override
        public Type<ServerConfig> type() {
            return TYPE;
        }
    }
}
