package bau5.mods.projectbench.common;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.packet.Packet;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.common.ISidedInventory;
import cpw.mods.fml.common.network.PacketDispatcher;

public class TileEntityProjectBench extends TileEntity implements IInventory, ISidedInventory
{
	class LocalInventoryCrafting extends InventoryCrafting
	{
		public LocalInventoryCrafting() {
			super(new Container(){
				public boolean canInteractWith(EntityPlayer var1) {
					return false;
				}
			}, 3, 3);
		}
		public Container eventHandler;
	};
	
	private ItemStack[] inv;
	private Packet nextPacket;
	private boolean shouldUpdate = false; 
	public IInventory craftResult;
	public IInventory craftSupplyMatrix;
	public LocalInventoryCrafting craftMatrix;
	private ItemStack result = null;
	private ItemStack lastResult;
	private int sync = 0;
	
	public void onInventoryChanged()
	{
		super.onInventoryChanged();
	}
	public TileEntityProjectBench()
	{
		craftSupplyMatrix = new InventoryBasic("pbCraftingSupply", true, 18);
		craftResult = new InventoryCraftResult();
		inv = new ItemStack[27];
		shouldUpdate = true;
		craftMatrix = new LocalInventoryCrafting();
	}
	public ItemStack findRecipe(boolean fromPacket) 
	{
		lastResult = result;
		if(!shouldUpdate){
			return getResult();
		}
		shouldUpdate = false;
		
		ItemStack stack = null;
		for (int i = 0; i < craftMatrix.getSizeInventory(); ++i) 
		{
			stack = getStackInSlot(i);
			craftMatrix.setInventorySlotContents(i, stack);
		}

		ItemStack recipe = CraftingManager.getInstance().findMatchingRecipe(craftMatrix, worldObj);
		
		setResult(recipe);
		updateResultSlot();
		
//		System.out.println("Actually finding recipe. " +result);
		if(!fromPacket && lastResult != result)
			nextPacket = PBPacketHandler.prepPacketMkI(this);
		return recipe;
	}
	
	public void markShouldUpdate(){
		shouldUpdate = true;
	}
	public void updateResultSlot(){
		craftResult.setInventorySlotContents(0, result);
	}
	
	public ItemStack[] getCrafting()
	{
		ItemStack[] craftings = new ItemStack[9];
		for(int i = 0; i < 9; i++)
		{
			craftings[i] = inv[i];
		}
		return craftings;
	}
	
	public ItemStack getResult()
	{
		return (result == null) ? null : result.copy();
	}
	public void setResult(ItemStack stack)
	{
		if(stack != null)
			result = stack.copy();
		else
			result = null;
		updateResultSlot();
	}
	
	@Override
	public int getSizeInventory() 
	{
		return inv.length;
	}

	@Override
	public ItemStack getStackInSlot(int slot) 
	{
		return inv[slot];
	}

	@Override
	public ItemStack decrStackSize(int slot, int amount) 
	{
		ItemStack stack = getStackInSlot(slot);
		if(stack != null)
		{
			if(stack.stackSize <= amount)
			{
				setInventorySlotContents(slot, null);
			} else
			{
				stack = stack.splitStack(amount);
				if(stack.stackSize == 0) 
				{
					setInventorySlotContents(slot, null);
				}
			}
		}
		if(slot <= 9)
			markShouldUpdate();
		onInventoryChanged();
		return stack;
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int slot) 
	{
		ItemStack stack = getStackInSlot(slot);
		if(stack != null)
		{
			setInventorySlotContents(slot, null);
		}
		onInventoryChanged();
		return stack;
	}

	@Override
	public void setInventorySlotContents(int slot, ItemStack stack) 
	{
		inv[slot] = stack;
		if(stack != null && stack.stackSize > getInventoryStackLimit())
		{
			stack.stackSize = getInventoryStackLimit();
		}
		if(slot <= 9)
			markShouldUpdate();
		onInventoryChanged();
	}

	@Override
	public String getInvName()
	{
		return "Project Bench";
	}

	@Override
	public int getInventoryStackLimit() 
	{
		return 64;
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer player) 
	{
		return worldObj.getBlockTileEntity(xCoord, yCoord, zCoord) == this &&
				player.getDistanceSq(xCoord +0.5, yCoord +0.5, zCoord +0.5) < 64;
	}
	
	public int[] getRecipeStacksForPacket()
	{
		ItemStack result = null;
		if(shouldUpdate){
			result = findRecipe(true);
		}
		else{
			result = this.result;
		}
		if(result != null)
		{
			int[] craftingStacks = new int[27];
			int index = 0;
			for(int i = 0; i < 9; i++)
			{
				if(inv[i] != null)
				{
					craftingStacks[index++] = inv[i].itemID;
					craftingStacks[index++] = inv[i].stackSize;
					craftingStacks[index++] = inv[i].getItemDamage();
				} else
				{
					craftingStacks[index++] = 0;
					craftingStacks[index++] = 0;
					craftingStacks[index++] = 0;
				}
			}
			return craftingStacks;
		} else
			return null;
	}

	public void buildResultFromPacket(int[] stacksData)
	{
		if(stacksData == null)
		{
			this.setResult(null);
			return;
		}
		if(stacksData.length != 0)
		{
			int index = 0;
			for(int i = 0; i < 9; i++)
			{
				if(stacksData[index + 1] != 0)
				{
					ItemStack stack = new ItemStack(stacksData[index], stacksData[index+1], stacksData[index+2]);
					inv[i] = stack;
				}
				else
				{
					inv[i] = null;
				}
				index = index + 3;
			}
			shouldUpdate = true;
			findRecipe(true);
		} else
			this.setResult(null);
	}
	@Override
	public Packet getDescriptionPacket()
	{
		return PBPacketHandler.prepPacketMkI(this);
	}

	@Override
	public void openChest() {}

	@Override
	public void closeChest() {}

	public void updateEntity()
    {
		super.updateEntity();
		
		if(++sync % 40 == 0){
			if(nextPacket != null){
				PacketDispatcher.sendPacketToAllAround(xCoord, yCoord, zCoord, 20,
							   worldObj.getWorldInfo().getDimension(), nextPacket);
//				System.out.println("Sent packet.");
				nextPacket = null;
			}
		}
		if(sync > 6000){
			PacketDispatcher.sendPacketToAllInDimension(getDescriptionPacket(),
					worldObj.getWorldInfo().getDimension());
			sync = 0;
		}
    }
	
	@Override
	public void readFromNBT(NBTTagCompound tagCompound)
	{
		super.readFromNBT(tagCompound);
		
		NBTTagList tagList = tagCompound.getTagList("Inventory");
		for(int i = 0; i < tagList.tagCount(); i++)
		{
			NBTTagCompound tag = (NBTTagCompound) tagList.tagAt(i);
			byte slot = tag.getByte("Slot");
			if(slot >= 0 && slot < inv.length)
			{
				inv[slot] = ItemStack.loadItemStackFromNBT(tag);
			}
		}
	}
	@Override
	public void writeToNBT(NBTTagCompound tagCompound)
	{
		super.writeToNBT(tagCompound);
		
		NBTTagList itemList = new NBTTagList();	
		
		for(int i = 0; i < inv.length; i++)
		{
			ItemStack stack = inv[i];
			if(stack != null)
			{
				NBTTagCompound tag = new NBTTagCompound();	
				tag.setByte("Slot", (byte)i);
				stack.writeToNBT(tag);
				itemList.appendTag(tag);
			}
		}
		tagCompound.setTag("Inventory", itemList);
	}
	//TODO Update for vanilla sided inventory eventually.
	/*
	//Get start inventory side
	@Override
	public int func_94127_c(int side) {
		switch(side)
		{
		case 0: return 0;
		default: return 9;
		}
	}
	//Get size inventory based on side
	@Override
	public int func_94128_d(int side) {
		switch(side)
		{
		case 0: return 9;
		default: return 18;
		}
	}*/
	@Override
	public int getStartInventorySide(ForgeDirection side) 
	{
		switch(side)
		{
		case UP: return 0;
		default: return 9;
		}
	}
	@Override
	public int getSizeInventorySide(ForgeDirection side) 
	{
		switch(side)
		{
		case UP: return 9;
		default: return 18;
		}
	}
	@Override
	public boolean isInvNameLocalized() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean isStackValidForSlot(int i, ItemStack itemstack) {
		// TODO Auto-generated method stub
		return false;
	}
}
