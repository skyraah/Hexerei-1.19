package net.joefoxe.hexerei.tileentity;

import net.joefoxe.hexerei.Hexerei;
import net.joefoxe.hexerei.data.recipes.DryingRackRecipe;
import net.joefoxe.hexerei.util.HexereiPacketHandler;
import net.joefoxe.hexerei.util.message.TESyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class DryingRackTile extends RandomizableContainerBlockEntity implements WorldlyContainer, Clearable, MenuProvider {

    //    public final ItemStackHandler itemHandler = createHandler();
//    private final LazyOptional<IItemHandler> handler = LazyOptional.of(() -> itemHandler);
    protected NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);

    public boolean[] crafted = {false, false, false};
    public boolean[] crafting = {false, false, false};
    public int[] dryingTimeMax = {200, 200, 200};
    public int[] dryingTime = {200, 200, 200};
    public int[] placedTime = {0, 0, 0};
    public ItemStack[] output = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};

    public DryingRackTile(BlockEntityType<?> tileEntityTypeIn, BlockPos blockPos, BlockState blockState) {
        super(tileEntityTypeIn, blockPos, blockState);
    }

    @Override
    public NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    public void setItems(NonNullList<ItemStack> itemsIn) {
        this.items = itemsIn;
    }

    @Override
    public int getMaxStackSize() {
        return 3;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        sync();
    }

    public void sync() {

        if (level != null) {
            if (!level.isClientSide)
                HexereiPacketHandler.instance.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(worldPosition)), new TESyncPacket(worldPosition, save(new CompoundTag())));

            if (level != null)
                this.level.sendBlockUpdated(this.worldPosition, this.level.getBlockState(this.worldPosition), this.level.getBlockState(this.worldPosition),
                        Block.UPDATE_CLIENTS);
        }
    }

    LazyOptional<? extends IItemHandler>[] handlers =
            SidedInvWrapper.create(this, Direction.UP, Direction.DOWN, Direction.NORTH);

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction facing) {
        if (facing != null && capability == ForgeCapabilities.ITEM_HANDLER) {
            return switch (facing) {
                case UP -> handlers[0].cast();
                case DOWN -> handlers[1].cast();
                default -> handlers[2].cast();
            };
        }

        return super.getCapability(capability, facing);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap) {

        return super.getCapability(cap);
    }

    public Item getItemInSlot(int slot) {
        return this.items.get(slot).getItem();
    }

    public int getNumberOfItems() {

        int num = 0;
        for (int i = 0; i < 8; i++) {
            if (this.items.get(i) != ItemStack.EMPTY)
                num++;
        }
        return num;

    }


    @Override
    public void deserializeNBT(CompoundTag nbt) {
        super.deserializeNBT(nbt);
    }

    @Override
    public CompoundTag serializeNBT() {
        return super.serializeNBT();
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
    }

    @Override
    public void onLoad() {
        super.onLoad();
    }


    public DryingRackTile(BlockPos blockPos, BlockState blockState) {
        this(ModTileEntities.DRYING_RACK_TILE.get(), blockPos, blockState);
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        if (index >= 0 && index < this.items.size()) {
            ItemStack itemStack = stack.copy();
            this.items.set(index, itemStack);
            if (index == 0)
                dryingTime[0] = dryingTimeMax[0];
            if (index == 1)
                dryingTime[1] = dryingTimeMax[1];
            if (index == 2)
                dryingTime[2] = dryingTimeMax[2];
            level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, this.level.random.nextFloat() * 0.4F + 1.0F);
        }

        sync();
    }

    @Override
    public ItemStack removeItem(int index, int p_59614_) {
        this.unpackLootTable(null);
        ItemStack itemstack = ContainerHelper.removeItem(this.getItems(), index, p_59614_);
        if (!itemstack.isEmpty()) {
            this.setChanged();
            if (index == 0) {
                crafted[0] = false;
                sync();
            }
            if (index == 0) {
                crafted[1] = false;
                sync();
            }
            if (index == 0) {
                crafted[2] = false;
                sync();
            }
        }


        return itemstack;
    }

    public void craft() {
        List<SimpleContainer> inv = new ArrayList<>();
        inv.add(new SimpleContainer(1));
        inv.add(new SimpleContainer(1));
        inv.add(new SimpleContainer(1));
        for (int i = 0; i < 3; i++) {
            inv.get(i).setItem(0, this.items.get(i));

            Optional<DryingRackRecipe> recipe = level.getRecipeManager()
                    .getRecipeFor(DryingRackRecipe.Type.INSTANCE, inv.get(i), level);

            BlockEntity blockEntity = level.getBlockEntity(this.worldPosition);
            if (blockEntity instanceof DryingRackTile) {
                final int j = i;
                recipe.ifPresent(iRecipe -> {
                    ItemStack recipeOutput = iRecipe.getResultItem(this.level.registryAccess());
                    ItemStack input = iRecipe.getIngredients().get(0).getItems()[0];

                    if (input.getItem() == this.items.get(j).getItem()) {
                        // FIRST SLOT MATCHES

                        if (!crafting[j]) {
                            crafting[j] = true;
                            output[j] = recipeOutput.copy();
                            dryingTimeMax[j] = iRecipe.getDryingTime();
                            dryingTime[j] = dryingTimeMax[j];
                            sync();
                        }

                    } else {
                        if (crafting[j]) {
                            crafting[j] = false;
                            sync();
                        }
                    }
//
//                    if (input.getItem() == this.items.get(j).getItem()) {
//                        // SECOND SLOT MATCHES
//
//                        if (!crafting[j]) {
//                            crafting[j] = true;
//                            output[j] = recipeOutput.copy();;
//                            dryingTimeMax[j] = iRecipe.getDryingTime();
//                            dryingTime[j] = dryingTimeMax[j];
//                            sync();
//                        }
//
//                    } else {
//                        if (crafting[j]) {
//                            crafting[j] = false;
//                            sync();
//                        }
//                    }
//
//                    if (input.getItem() == this.items.get(2).getItem()) {
//                        // THIRD SLOT MATCHES
//
//                        if (!crafting[2]) {
//                            crafting[2] = true;
//                            output[2] = recipeOutput.copy();
//                            dryingTimeMax[2] = iRecipe.getDryingTime();
//                            dryingTime[2] = dryingTimeMax[2];
//                            sync();
//                        }
//
//                    } else {
//                        if (crafting[2]) {
//                            crafting[2] = false;
//                            sync();
//                        }
//                    }

                });
            }
        }


//

    }


    private void craftTheItem(ItemStack output, int slot) {
        output.setCount(this.items.get(slot).getCount());
        this.setItem(slot, output);
    }


    @Override
    public void load(CompoundTag nbt) {
//        itemHandler.deserializeNBT(nbt.getCompound("inv"));
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        if (!this.tryLoadLootTable(nbt)) {
            ContainerHelper.loadAllItems(nbt, this.items);
        }
//        super.read(state, nbt);
//        if (nbt.contains("CustomName", 8))
//            this.customName = Component.Serializer.fromJson(nbt.getString("CustomName"));

        if (nbt.contains("dryingTime[0]", Tag.TAG_INT))
            dryingTime[0] = nbt.getInt("dryingTime[0]");
        if (nbt.contains("dryingTime[1]", Tag.TAG_INT))
            dryingTime[1] = nbt.getInt("dryingTime[1]");
        if (nbt.contains("dryingTime[2]", Tag.TAG_INT))
            dryingTime[2] = nbt.getInt("dryingTime[2]");
        if (nbt.contains("crafting[0]", Tag.TAG_INT))
            crafting[0] = nbt.getInt("crafting[0]") == 1;
        if (nbt.contains("crafting[1]", Tag.TAG_INT))
            crafting[1] = nbt.getInt("crafting[1]") == 1;
        if (nbt.contains("crafting[2]", Tag.TAG_INT))
            crafting[2] = nbt.getInt("crafting[2]") == 1;
        if (nbt.contains("crafted[0]", Tag.TAG_INT))
            crafted[0] = nbt.getInt("crafted[0]") == 1;
        if (nbt.contains("crafted[1]", Tag.TAG_INT))
            crafted[1] = nbt.getInt("crafted[1]") == 1;
        if (nbt.contains("crafted[2]", Tag.TAG_INT))
            crafted[2] = nbt.getInt("crafted[2]") == 1;

        if (nbt.contains("placedTime[0]", Tag.TAG_INT))
            placedTime[0] = nbt.getInt("placedTime[0]");
        if (nbt.contains("placedTime[1]", Tag.TAG_INT))
            placedTime[1] = nbt.getInt("placedTime[1]");
        if (nbt.contains("placedTime[2]", Tag.TAG_INT))
            placedTime[2] = nbt.getInt("placedTime[2]");

        if (nbt.contains("output[0]"))
            output[0] = ItemStack.of(nbt.getCompound("output[0]"));
        if (nbt.contains("output[1]"))
            output[1] = ItemStack.of(nbt.getCompound("output[1]"));
        if (nbt.contains("output[2]"))
            output[2] = ItemStack.of(nbt.getCompound("output[2]"));
        super.load(nbt);

    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container." + Hexerei.MOD_ID + ".dipper");
    }

    @Override
    protected AbstractContainerMenu createMenu(int p_58627_, Inventory p_58628_) {
        return null;
    }

    @Override
    public void saveAdditional(CompoundTag compound) {
        super.saveAdditional(compound);
        ContainerHelper.saveAllItems(compound, this.items);

        compound.putInt("dryingTime[0]", dryingTime[0]);
        compound.putInt("dryingTime[1]", dryingTime[1]);
        compound.putInt("dryingTime[2]", dryingTime[2]);

        compound.putInt("crafted[0]", crafted[0] ? 1 : 0);
        compound.putInt("crafted[1]", crafted[1] ? 1 : 0);
        compound.putInt("crafted[2]", crafted[2] ? 1 : 0);

        compound.putInt("crafting[0]", crafting[0] ? 1 : 0);
        compound.putInt("crafting[1]", crafting[1] ? 1 : 0);
        compound.putInt("crafting[2]", crafting[2] ? 1 : 0);

        compound.putInt("placedTime[0]", placedTime[0]);
        compound.putInt("placedTime[1]", placedTime[1]);
        compound.putInt("placedTime[2]", placedTime[2]);

        compound.put("output[0]", output[0].save(new CompoundTag()));
        compound.put("output[1]", output[1].save(new CompoundTag()));
        compound.put("output[2]", output[2].save(new CompoundTag()));
    }


    //    @Override
    public CompoundTag save(CompoundTag compound) {
        super.saveAdditional(compound);
//        compound.put("inv", itemHandler.serializeNBT());
//        if (this.customName != null)
//            compound.putString("CustomName", Component.Serializer.toJson(this.customName));
        ContainerHelper.saveAllItems(compound, this.items);

        compound.putInt("dryingTime[0]", dryingTime[0]);
        compound.putInt("dryingTime[1]", dryingTime[1]);
        compound.putInt("dryingTime[2]", dryingTime[2]);

        compound.putInt("crafted[0]", crafted[0] ? 1 : 0);
        compound.putInt("crafted[1]", crafted[1] ? 1 : 0);
        compound.putInt("crafted[2]", crafted[2] ? 1 : 0);

        compound.putInt("crafting[0]", crafting[0] ? 1 : 0);
        compound.putInt("crafting[1]", crafting[1] ? 1 : 0);
        compound.putInt("crafting[2]", crafting[2] ? 1 : 0);

        compound.putInt("placedTime[0]", placedTime[0]);
        compound.putInt("placedTime[1]", placedTime[1]);
        compound.putInt("placedTime[2]", placedTime[2]);

        compound.put("output[0]", output[0].save(new CompoundTag()));
        compound.put("output[1]", output[1].save(new CompoundTag()));
        compound.put("output[2]", output[2].save(new CompoundTag()));

        return compound;
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.save(new CompoundTag());
    }

    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket() {

        return ClientboundBlockEntityDataPacket.create(this, (tag) -> this.getUpdateTag());
    }

    @Override
    public void onDataPacket(final Connection net, final ClientboundBlockEntityDataPacket pkt) {
        this.deserializeNBT(pkt.getTag());
    }

    public static double getDistanceToEntity(Entity entity, BlockPos pos) {
        double deltaX = entity.position().x() - pos.getX() - 0.5f;
        double deltaY = entity.position().y() - pos.getY() - 0.5f;
        double deltaZ = entity.position().z() - pos.getZ() - 0.5f;

        return Math.sqrt((deltaX * deltaX) + (deltaY * deltaY) + (deltaZ * deltaZ));
    }

    public static double getDistance(float x1, float y1, float x2, float y2) {
        double deltaX = x2 - x1;
        double deltaY = y2 - y1;

        return Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));
    }


//    @Override
//    public double getMaxRenderDistanceSquared() {
//        return 4096D;
//    }

    @Override
    public AABB getRenderBoundingBox() {
        return super.getRenderBoundingBox().inflate(5, 5, 5);
    }

    public float getAngle(Vec3 pos) {
        float angle = (float) Math.toDegrees(Math.atan2(pos.z() - this.getBlockPos().getZ() - 0.5f, pos.x() - this.getBlockPos().getX() - 0.5f));

        if (angle < 0) {
            angle += 360;
        }

        return angle;
    }

    public float getSpeed(double pos, double posTo) {
        return (float) (0.01f + 0.10f * (Math.abs(pos - posTo) / 3f));
    }

    public Vec3 rotateAroundVec(Vec3 vector3dCenter, float rotation, Vec3 vector3d) {
        Vec3 newVec = vector3d.subtract(vector3dCenter);
        newVec = newVec.yRot(rotation / 180f * (float) Math.PI);
        newVec = newVec.add(vector3dCenter);

        return newVec;
    }

    public int putItems(int slot, @Nonnull ItemStack stack) {
        ItemStack stack1 = stack.copy();
        Random rand = new Random();
        if (this.items.get(slot).isEmpty()) {

            if (stack1.getCount() > 2) {
                stack1.setCount(3);
                this.items.set(slot, stack1);
                sync();
                stack.shrink(3);

                for (int i = 0; i < 3; i++)
                    level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, rand.nextFloat() * 0.4F + 1.0F);
            } else {
                this.items.set(slot, stack1);
                sync();
                stack.shrink(stack.getCount());

                for (int i = 0; i < stack1.getCount(); i++)
                    level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, rand.nextFloat() * 0.4F + 1.0F);
            }
            return 1;
        }

        if (!ItemStack.isSameItemSameTags(stack, this.items.get(slot)))
            return 0;

        int count = this.items.get(slot).getCount();

        if (stack1.getCount() > 2 - count) {
            stack1.setCount(3);
            this.items.set(slot, stack1);
            sync();
            stack.shrink(3 - count);

            for (int i = 0; i < 3 - count; i++)
                level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, rand.nextFloat() * 0.4F + 1.0F);
        } else {
            stack1.setCount(count + stack1.getCount());
            this.items.set(slot, stack1);
            sync();
            stack.shrink(stack.getCount());

            for (int i = 0; i < stack1.getCount(); i++)
                level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0F, rand.nextFloat() * 0.4F + 1.0F);
        }


        return 1;
    }

    public int interactDryingRack(Player player, BlockHitResult hit) {
        if (level == null) return 0;
        if (!player.isShiftKeyDown()) {
            if (!player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
                Random rand = new Random();
                if (this.items.get(0).isEmpty() || (player.getItemInHand(InteractionHand.MAIN_HAND) == this.items.get(0))) {
                    putItems(0, player.getItemInHand(InteractionHand.MAIN_HAND));
                    dryingTime[0] = dryingTimeMax[0];
                    crafted[0] = false;

                    return 1;
                } else if (ItemStack.isSameItemSameTags(player.getItemInHand(InteractionHand.MAIN_HAND), this.items.get(0)) && this.items.get(0).getCount() < getMaxStackSize()) {
                    putItems(0, player.getItemInHand(InteractionHand.MAIN_HAND));
                } else if (this.items.get(1).isEmpty() || (player.getItemInHand(InteractionHand.MAIN_HAND) == this.items.get(1) && this.items.get(1).getCount() < getMaxStackSize())) {
                    putItems(1, player.getItemInHand(InteractionHand.MAIN_HAND));
                    dryingTime[1] = dryingTimeMax[1];
                    crafted[1] = false;

                    return 1;
                } else if (ItemStack.isSameItemSameTags(player.getItemInHand(InteractionHand.MAIN_HAND), this.items.get(1)) && this.items.get(1).getCount() < getMaxStackSize()) {
                    putItems(1, player.getItemInHand(InteractionHand.MAIN_HAND));
                } else if (this.items.get(2).isEmpty() || (player.getItemInHand(InteractionHand.MAIN_HAND) == this.items.get(2) && this.items.get(2).getCount() < getMaxStackSize())) {
                    putItems(2, player.getItemInHand(InteractionHand.MAIN_HAND));
                    dryingTime[2] = dryingTimeMax[2];
                    crafted[2] = false;

                    return 1;
                } else if (ItemStack.isSameItemSameTags(player.getItemInHand(InteractionHand.MAIN_HAND), this.items.get(2)) && this.items.get(2).getCount() < getMaxStackSize()) {
                    putItems(2, player.getItemInHand(InteractionHand.MAIN_HAND));
                }
            }
            if (crafted[0]) {
                crafted[0] = false;
                dryingTime[0] = dryingTimeMax[0];
                player.inventory.placeItemBackInInventory(this.items.get(0).copy());
                level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, level.random.nextFloat() * 0.4F + 1.0F);
                this.items.set(0, ItemStack.EMPTY);
                output[0] = ItemStack.EMPTY;
            }
            if (crafted[1]) {
                crafted[1] = false;
                dryingTime[1] = dryingTimeMax[1];
                player.inventory.placeItemBackInInventory(this.items.get(1).copy());
                level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, level.random.nextFloat() * 0.4F + 1.0F);
                this.items.set(1, ItemStack.EMPTY);
                output[1] = ItemStack.EMPTY;
            }
            if (crafted[2]) {
                crafted[2] = false;
                dryingTime[2] = dryingTimeMax[2];
                player.inventory.placeItemBackInInventory(this.items.get(2).copy());
                level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, level.random.nextFloat() * 0.4F + 1.0F);
                this.items.set(2, ItemStack.EMPTY);
                output[2] = ItemStack.EMPTY;
            }
            sync();
        } else {
            if (!crafting[0]) {

                crafted[0] = false;
                dryingTime[0] = dryingTimeMax[0];
                player.inventory.placeItemBackInInventory(this.items.get(0).copy());
                level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, level.random.nextFloat() * 0.4F + 1.0F);
                this.items.set(0, ItemStack.EMPTY);
                output[0] = ItemStack.EMPTY;
                sync();
            }
            if (!this.items.get(1).isEmpty() && !crafting[1]) {

                crafted[1] = false;
                dryingTime[1] = dryingTimeMax[1];
                player.inventory.placeItemBackInInventory(this.items.get(1).copy());
                level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, level.random.nextFloat() * 0.4F + 1.0F);
                this.items.set(1, ItemStack.EMPTY);
                output[1] = ItemStack.EMPTY;
                sync();
            }
            if (!this.items.get(2).isEmpty() && !crafting[2]) {

                crafted[2] = false;
                dryingTime[2] = dryingTimeMax[2];
                player.inventory.placeItemBackInInventory(this.items.get(2).copy());
                level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, level.random.nextFloat() * 0.4F + 1.0F);
                this.items.set(2, ItemStack.EMPTY);
                output[2] = ItemStack.EMPTY;
                sync();
            }
        }

        return 0;
    }

    //    @Override
    public void tick() {

        if (level instanceof ServerLevel) {
            craft();
            for (int i = 0; i < 3; i++) {
                if (items.get(i).isEmpty())
                    placedTime[i] = 0;
                else
                    placedTime[i]++;
                if (crafting[i]) {
                    if (dryingTime[i] > 0)
                        dryingTime[i]--;
                    if (dryingTime[i] == 0) {
                        crafted[i] = true;
                        crafting[i] = false;
                        craftTheItem(output[i], i);
                    }
                }
            }
        }


    }

    @Override
    public int[] getSlotsForFace(Direction p_19238_) {
        return new int[]{0, 1, 2};
    }

    public boolean canPlaceItemThroughFace(int index, ItemStack itemStackIn, @Nullable Direction direction) {
        return this.canPlaceItem(index, itemStackIn);
    }

    public boolean canPlaceItem(int index, ItemStack stack) {
        return this.items.get(index).isEmpty() || this.items.get(index).getCount() < getMaxStackSize();
    }

    @Override
    public boolean canTakeItemThroughFace(int p_19239_, ItemStack p_19240_, Direction p_19241_) {
        return !crafting[p_19239_] && placedTime[p_19239_] >= 20;
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

}
