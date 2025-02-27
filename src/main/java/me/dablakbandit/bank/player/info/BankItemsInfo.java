package me.dablakbandit.bank.player.info;

import java.util.*;

import me.dablakbandit.bank.config.path.impl.BankPermissionStringListPath;
import me.dablakbandit.bank.implementations.def.ItemDefault;
import me.dablakbandit.bank.implementations.def.ItemDefaultImplementation;
import me.dablakbandit.bank.log.BankLog;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissionAttachmentInfo;

import me.dablakbandit.bank.config.BankPermissionConfiguration;
import me.dablakbandit.bank.config.BankPluginConfiguration;
import me.dablakbandit.bank.implementations.blacklist.ItemBlacklistImplementation;
import me.dablakbandit.core.players.CorePlayers;
import me.dablakbandit.core.players.info.JSONInfo;
import me.dablakbandit.core.utils.ItemUtils;
import me.dablakbandit.core.utils.Version;
import me.dablakbandit.core.utils.itemutils.IItemUtils;
import me.dablakbandit.core.utils.json.strategy.Exclude;
import me.dablakbandit.core.vault.Eco;

public class BankItemsInfo extends IBankInfo implements JSONInfo, PermissionsInfo, BankDefaultInfo{
	
	private static final IItemUtils				itemUtils		= ItemUtils.getInstance();
	
	private final Map<Integer, List<ItemStack>>	itemMap			= Collections.synchronizedMap(new HashMap<>());
	private final Map<Integer, ItemStack>		tabItemMap		= Collections.synchronizedMap(new HashMap<>());
	private final Map<Integer, String>			tabNameMap		= Collections.synchronizedMap(new HashMap<>());
	protected final Map<Integer, Integer>		boughtSlotsMap	= Collections.synchronizedMap(new HashMap<>());
	private int									openTab			= 1;
	private int									scrolled		= 0;
	private int									boughtTabs;
	private int									commandSlots, permissionSlots;
	@Exclude
	private int									buySlots, buyTabs, totalTabCount;
	
	public BankItemsInfo(CorePlayers pl){
		super(pl);
		for(int i = 1; i <= 9; i++){
			itemMap.computeIfAbsent(i, ArrayList::new);
		}
	}
	
	public Map<Integer, List<ItemStack>> getItemMap(){
		return itemMap;
	}
	
	public Map<Integer, ItemStack> getTabItemMap(){
		return tabItemMap;
	}

	public Map<Integer, String> getTabNameMap() {
		return tabNameMap;
	}

	public int getOpenTab(){
		return openTab;
	}
	
	public void setOpenTab(int openTab){
		this.openTab = openTab;
	}
	
	public int getScrolled(){
		return scrolled;
	}
	
	public void addScrolled(int add){
		this.scrolled = Math.max(0, this.scrolled + add);
	}
	
	public void setScrolled(int scrolled){
		this.scrolled = Math.max(0, scrolled);
	}
	
	public int getBoughtTabs(){
		return boughtTabs;
	}
	
	public void setBoughtTabs(int boughtTabs){
		this.boughtTabs = boughtTabs;
	}
	
	public int getCommandSlots(){
		return commandSlots;
	}
	
	public void addCommandSlots(int commandSlots){
		this.commandSlots += commandSlots;
	}
	
	public void setCommandSlots(int commandSlots){
		this.commandSlots = commandSlots;
	}
	
	public int getPermissionSlots(){
		return permissionSlots;
	}
	
	@Deprecated
	public void setPermissionSlots(int permissionSlots){
		this.permissionSlots = permissionSlots;
	}
	
	public int getBuySlots(){
		return buySlots;
	}
	
	public void incrementBuySlots(){
		if(getBoughtSlots(openTab) + buySlots < BankPluginConfiguration.BANK_ITEMS_SLOTS_BUY_MAX.get()){
			this.buySlots++;
		}
	}
	
	public void decrementBuySlots(){
		this.buySlots = Math.max(0, this.buySlots - 1);
	}
	
	public int getBuyTabs(){
		return buyTabs;
	}
	
	public void incrementBuyTabs(){
		if(totalTabCount + buyTabs < 9){
			this.buyTabs++;
		}
	}
	
	public void decrementBuyTabs(){
		this.buyTabs = Math.max(0, this.buyTabs - 1);
	}
	
	public Map<Integer, Integer> getBoughtSlotsMap(){
		return boughtSlotsMap;
	}
	
	public int getTotalTabCount(){
		return totalTabCount;
	}
	
	@Override
	public void jsonInit(){
		for(int i = 1; i <= 9; i++){
			itemMap.computeIfAbsent(i, ArrayList::new);
		}
		itemMap.values().forEach(l -> l.removeIf(is -> is == null || is.getType() == Material.AIR));
	}
	
	@Override
	public void jsonFinal(){
		
	}
	
	public void addInventoryToBank(Player player, boolean force){
		addToBank(player, 9, 36, force);
	}
	
	public void addAllInventoryToBank(Player player, boolean force){
		addToBank(player, 0, 36, force);
		addOffhandToBank(player, force);
	}
	
	public void addHotbarToBank(Player player, boolean force){
		addToBank(player, 0, 9, force);
		addOffhandToBank(player, force);
	}
	
	public void addOffhandToBank(Player player, boolean force){
		if(!Version.isAtleastNine()){ return; }
		ItemStack is = player.getInventory().getItemInOffHand();
		if(isEmpty(is)){ return; }
		player.getInventory().setItemInOffHand(addBankItem(player, is, force));
	}
	
	protected void addToBank(Player player, int x, int z, boolean force){
		Inventory inv = player.getInventory();
		for(int i = x; i < z; i++){
			ItemStack is = inv.getItem(i);
			if(isEmpty(is)){
				continue;
			}
			is = addBankItem(player, is, force);
			inv.setItem(i, is);
			if(is != null){ return; }
		}
	}
	
	public ItemStack addBankItem(Player player, ItemStack is, boolean force){
		return addBankItem(player, is, openTab, force);
	}
	
	public ItemStack addBankItem(Player player, ItemStack is, int tab, boolean force){
		if(!BankPermissionConfiguration.PERMISSION_ITEMS_DEPOSIT.has(player)){
			return is;
		}
		if(isEmpty(is)){ return is; }
		if(ItemBlacklistImplementation.getInstance().isBlacklisted(is)){
			// pl.getPlayer().sendMessage(LanguageConfiguration.MESSAGE_ITEM_IS_BLACKLISTED.getMessage());
			return is;
		}
		if(!force && tab > totalTabCount){ return is; }
		int itemSize = is.getAmount();
		is = mergeBank(is, tab);
		if(isEmpty(is)){
			save(BankPluginConfiguration.BANK_SAVE_ITEM_DEPOSIT);
			return null;
		}
		int size = getTabSize(openTab);
		if(force || size < BankPluginConfiguration.BANK_ITEMS_TABS_SIZE_MAX.get()){
			if(force || getTotalBankSize(tab) < getBankSlots(tab)){
				itemMap.get(tab).add(is);
				save(BankPluginConfiguration.BANK_SAVE_ITEM_DEPOSIT);
				return null;
			}
		}
		if(is.getAmount() != itemSize){
			save(BankPluginConfiguration.BANK_SAVE_ITEM_DEPOSIT);
		}
		// player.sendMessage(LanguageConfiguration.MESSAGE_BANK_IS_FULL.getMessage());
		return is;
	}
	
	protected ItemStack mergeBank(ItemStack is, int tab){
		int left = is.getAmount();
		int max = is.getMaxStackSize();
		for(ItemStack is1 : itemMap.get(tab)){
			if(!canMerge(is, is1)){
				continue;
			}
			int taken = max - is1.getAmount();
			if(taken > left){
				is1.setAmount(is1.getAmount() + left);
				return null;
			}else{
				left -= taken;
				is1.setAmount(max);
			}
		}
		is.setAmount(left);
		return is;
	}
	
	public boolean canMerge(ItemStack from, ItemStack to){
		if(from == null || to == null){ return false; }
		if(!itemUtils.canMerge(from, to)){ return false; }
		int max = to.getMaxStackSize();
		return to.getAmount() < max;
	}
	
	private boolean isEmpty(ItemStack is){
		return is == null || is.getType() == Material.AIR;
	}

	public int getTotalBankSize(int page){
		if(BankPluginConfiguration.BANK_ITEMS_SLOTS_BUY_PER_TAB.get()){ return itemMap.get(page).size(); }
		return itemMap.values().stream().map(List::size).mapToInt(Integer::intValue).sum();
	}
	public int getTabSize(int page){
		return itemMap.get(page).size();
	}
	
	public int getBoughtSlots(int page){
		return BankPluginConfiguration.BANK_ITEMS_SLOTS_BUY_PER_TAB.get() ? boughtSlotsMap.getOrDefault(page, 0) : boughtSlotsMap.getOrDefault(1, 0);
	}
	
	public int getBankSlots(int page){
		return BankPluginConfiguration.BANK_ITEMS_SLOTS_DEFAULT.get() + getBoughtSlots(page) + commandSlots + permissionSlots;
	}
	
	public int getBankSlots(){
		return getBankSlots(openTab);
	}
	
	public int getTotalSlots(){
		if(BankPluginConfiguration.BANK_ITEMS_TABS_ENABLED.get() && BankPluginConfiguration.BANK_ITEMS_SLOTS_BUY_PER_TAB.get()){
			int t = 0;
			for(int i = 1; i <= 9; i++){
				t += getBankSlots(i);
			}
			return t;
		}else{
			return getBankSlots(1);
		}
	}
	
	public int getTotalUsedSlots(){
		if(BankPluginConfiguration.BANK_ITEMS_TABS_ENABLED.get()){
			int t = 0;
			for(int i = 1; i <= 9; i++){
				t += getItems().get(i).size();
			}
			return t;
		}else{
			return getItems().get(1).size();
		}
	}
	
	public ItemStack getBankItemAtSlot(int slot, int page){
		List<ItemStack> listItems = getItems(page);
		if(slot >= listItems.size()){ return null; }
		return listItems.get(slot);
	}
	
	public List<ItemStack> getItems(int page){
		return itemMap.get(page);
	}
	
	private void removeBankItemAtInt(int slot, int page){
		List<ItemStack> listItems = itemMap.get(page);
		listItems.remove(slot);
	}
	
	public void removeBankToInventory(Player player){
		boolean taken = removeBankTo(player, 9, 36);
		if(taken){
			save(BankPluginConfiguration.BANK_SAVE_ITEM_WITHDRAW);
		}
	}
	
	public void removeAllBankToInventory(Player player){
		boolean taken = removeBankTo(player, 0, 36);
		taken |= removeBankToOffhand(player);
		if(taken){
			save(BankPluginConfiguration.BANK_SAVE_ITEM_WITHDRAW);
		}
	}
	
	public void removeBankToHotbar(Player player){
		boolean taken = false;
		taken |= removeBankTo(player, 0, 9);
		taken |= removeBankToOffhand(player);
		if(taken){
			save(BankPluginConfiguration.BANK_SAVE_ITEM_WITHDRAW);
		}
	}
	
	protected boolean removeBankTo(Player player, int x, int z){
		Inventory inv = player.getInventory();
		List<ItemStack> items = this.itemMap.get(this.openTab);
		Iterator<ItemStack> it = items.iterator();
		boolean taken = false;
		while(it.hasNext()){
			ItemStack is = it.next();
			for(int i = x; i < z; i++){
				ItemStack is1 = inv.getItem(i);
				if(isEmpty(is1)){
					inv.setItem(i, is);
					it.remove();
					taken = true;
					break;
				}else if(canMerge(is, is1)){
					int size = is.getAmount();
					int max = is1.getMaxStackSize();
					int b = max - is1.getAmount();
					if(b > size){
						is1.setAmount(is1.getAmount() + size);
						it.remove();
						taken = true;
						break;
					}else{
						size -= b;
						is1.setAmount(max);
						is.setAmount(size);
						taken = true;
					}
				}
			}
		}
		return taken;
	}
	
	private boolean removeBankToOffhand(Player player){
		if(!Version.isAtleastNine()){ return false; }
		List<ItemStack> items = this.itemMap.get(this.openTab);
		Iterator<ItemStack> it = items.iterator();
		boolean taken = false;
		while(it.hasNext()){
			ItemStack is = it.next();
			ItemStack is1 = player.getInventory().getItemInOffHand();
			if(isEmpty(is1)){
				player.getInventory().setItemInOffHand(is);
				it.remove();
				taken = true;
				break;
			}else if(canMerge(is, is1)){
				int size = is.getAmount();
				int max = is1.getMaxStackSize();
				int b = max - is1.getAmount();
				if(b > size){
					is1.setAmount(is1.getAmount() + size);
					it.remove();
					taken = true;
					break;
				}else{
					size -= b;
					is1.setAmount(max);
					is.setAmount(size);
					taken = true;
				}
			}
		}
		return taken;
	}
	
	private ItemStack mergeIntoInventory(Player player, ItemStack merge, int amount){
		if(merge == null){ return null; }
		Inventory inv = player.getInventory();
		for(int pSlot = 0; pSlot < 36; pSlot++){
			ItemStack is1 = inv.getItem(pSlot);
			if(!canMerge(merge, is1)){
				continue;
			}
			int size = Math.min(merge.getAmount(), amount);
			int max = is1.getMaxStackSize();
			int possible = max - is1.getAmount();
			if(possible >= size){
				is1.setAmount(is1.getAmount() + size);
				return null;
			}else{
				is1.setAmount(max);
				merge.setAmount(size - possible);
			}
		}
		return merge;
	}
	
	private ItemStack addIntoInventory(Player player, ItemStack add){
		if(add == null){ return null; }
		Inventory inv = player.getInventory();
		for(int i = 0; i < 36; i++){
			ItemStack inventoryItem = inv.getItem(i);
			if(isEmpty(inventoryItem)){
				inv.setItem(i, add);
				return null;
			}
		}
		return add;
	}
	
	private boolean takeBankItem(Player player, int tab, int slot, int take){
		if(!BankPermissionConfiguration.PERMISSION_ITEMS_WITHDRAW.has(player)){
			return false;
		}
		ItemStack is = getBankItemAtSlot(slot, tab);
		if(is == null){ return false; }
		int original = is.getAmount();
		int toTake = Math.min(is.getAmount(), take);
		int left = is.getAmount() - toTake;
		ItemStack copy = is.clone();
		copy.setAmount(toTake);
		copy = mergeIntoInventory(player, copy, toTake);
		copy = addIntoInventory(player, copy);
		left += copy != null ? copy.getAmount() : 0;
		if(left <= 0){
			removeBankItemAtInt(slot, tab);
			return true;
		}else{
			is.setAmount(left);
			return left != original;
		}
	}

	public boolean takeBankItemAt(Player player, int tab, int slot, int amount){
		boolean taken = takeBankItem(player, tab, slot, amount);
		if(taken){
			save(BankPluginConfiguration.BANK_SAVE_ITEM_WITHDRAW);
		}
		return taken;
	}
	
	public void sort(Comparator<ItemStack> comparator){
		sort(openTab, comparator);
	}
	
	public void sort(int page, Comparator<ItemStack> comparator){
		List<ItemStack> list = itemMap.get(page);
		list.sort(comparator);
		itemMap.put(page, list);
	}
	
	public boolean buySlots(int buySlots, CorePlayers pl){
		if(buySlots == 0){ return false; }
		double d = buySlots * BankPluginConfiguration.BANK_ITEMS_SLOTS_BUY_COST.get();
		if(Eco.getInstance().getEconomy().has(pl.getName(), d) && Eco.getInstance().getEconomy().withdrawPlayer(pl.getName(), d).transactionSuccess()){
			// player.sendMessage(LanguageConfiguration.MESSAGE_SLOTS_BOUGHT.getMessage().replace("<i>", "" + buy_slot_amount).replace("<p>", Format.formatMoney(d)));
			int slot = BankPluginConfiguration.BANK_ITEMS_SLOTS_BUY_PER_TAB.get() ? openTab : 1;
			boughtSlotsMap.put(slot, getBoughtSlots(slot) + buySlots);
			buySlots = 0;
			return true;
		}else{
			// player.sendMessage(LanguageConfiguration.MESSAGE_SLOTS_FAILED.getMessage());
		}
		return false;
	}
	
	public boolean buyTabs(CorePlayers pl){
		if(buyTabs == 0){ return false; }
		double d = buyTabs * BankPluginConfiguration.BANK_ITEMS_TABS_BUY_COST.get();
		if(Eco.getInstance().getEconomy().has(pl.getName(), d) && Eco.getInstance().getEconomy().withdrawPlayer(pl.getName(), d).transactionSuccess()){
			// player.sendMessage(LanguageConfiguration.MESSAGE_SLOTS_BOUGHT.getMessage().replace("<i>", "" + buy_slot_amount).replace("<p>", Format.formatMoney(d)));
			boughtTabs += buyTabs;
			buyTabs = 0;
			if(getPlayers().getPlayer() != null) {
				checkPermissions(getPlayers().getPlayer());
			}
			return true;
		}else{
			// player.sendMessage(LanguageConfiguration.MESSAGE_SLOTS_FAILED.getMessage());
		}
		return false;
	}
	
	public Map<Integer, List<ItemStack>> getItems(){
		return this.itemMap;
	}
	
	@Override
	public void checkPermissions(Permissible permissible){
		Collection<PermissionAttachmentInfo> permissions = permissible.getEffectivePermissions();
		if(!(permissible instanceof BankPermissionStringListPath.PathPermissible)) {
			List<Integer> maxList = BankPermissionConfiguration.PERMISSION_SLOTS.getValue(permissions);
			if (maxList.size() > 0) {
				if (BankPluginConfiguration.BANK_ITEMS_SLOTS_PERMISSION_COMBINE.get()) {
					permissionSlots = maxList.stream().mapToInt(Integer::intValue).sum();
				} else {
					permissionSlots = Collections.max(maxList);
				}
			} else {
				permissionSlots = 0;
			}
		}
		int tabCount = BankPluginConfiguration.BANK_ITEMS_TABS_DEFAULT.get() + boughtTabs;
		if(BankPluginConfiguration.BANK_ITEMS_TABS_PERMISSION_ENABLED.get()){
			List<Integer> tabsList = BankPermissionConfiguration.PERMISSION_TABS.getValue(permissions);
			if(BankPluginConfiguration.BANK_ITEMS_TABS_PERMISSION_COMBINE.get()){
				int sum = tabsList.stream().reduce(0, Integer::sum);
				Math.max(tabCount, sum + boughtTabs);
			}else{
				for(int tab : tabsList){
					tabCount = Math.max(tabCount, tab + boughtTabs);
				}
			}
		}
		tabCount = Math.max(0, Math.min(tabCount, 9));
		totalTabCount = tabCount;
	}

	@Override
	public void initDefault() {
		if(pl.getPlayer() != null && BankPluginConfiguration.BANK_ITEMS_DEFAULT_ENABLED.get()){
			for (ItemDefault itemDefault : ItemDefaultImplementation.getInstance().getDefault()) {
				addBankItem(pl.getPlayer(), itemDefault.getItemStack(), true);
			}
		}
	}
}
