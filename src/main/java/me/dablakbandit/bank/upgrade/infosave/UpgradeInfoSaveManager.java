package me.dablakbandit.bank.upgrade.infosave;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import me.dablakbandit.bank.BankPlugin;
import me.dablakbandit.bank.player.info.BankItemsInfo;
import org.bukkit.Bukkit;

import me.dablakbandit.bank.config.BankPluginConfiguration;
import me.dablakbandit.bank.database.base.IInfoDatabase;
import me.dablakbandit.bank.log.BankLog;
import me.dablakbandit.bank.player.BankPlayerManager;
import me.dablakbandit.bank.upgrade.infosave.database.IUpgradeInfoDatabase;
import me.dablakbandit.bank.upgrade.infosave.database.sqlite.UpgradeBankInfoSQLiteDatabase;
import me.dablakbandit.core.players.CorePlayers;
import me.dablakbandit.core.players.info.JSONInfo;
import org.bukkit.inventory.ItemStack;

public class UpgradeInfoSaveManager{
	
	private static final UpgradeInfoSaveManager upgrade = new UpgradeInfoSaveManager();
	
	public static UpgradeInfoSaveManager getInstance(){
		return upgrade;
	}
	
	private UpgradeInfoSaveManager(){
		
	}
	
	public void upgrade(IInfoDatabase infoDatabase){
		if(!infoDatabase.tableExists(infoDatabase.getDatabase().getConnection(), "bank_info_type")){ return; }
		IUpgradeInfoDatabase upgradeInfoDatabase = UpgradeBankInfoSQLiteDatabase.getInstance();
		infoDatabase.getDatabase().addListener(upgradeInfoDatabase);
		infoDatabase.getDatabase().addListener(upgradeInfoDatabase.getTypeDatabase());
		
		update(infoDatabase, upgradeInfoDatabase);
		try{
			infoDatabase.getDatabase().getConnection().prepareStatement("ALTER TABLE `bank_info_type` RENAME TO `backup_bank_info_type`;").execute();
			infoDatabase.getDatabase().getConnection().prepareStatement("ALTER TABLE `bank_player_info` RENAME TO `backup_bank_player_info`;").execute();
		}catch(SQLException throwables){
			throwables.printStackTrace();
		}
		
		Bukkit.shutdown();
	}
	
	private void update(IInfoDatabase infoDatabase, IUpgradeInfoDatabase upgradeInfoDatabase){
		BankLog.info(BankPluginConfiguration.BANK_LOG_PLUGIN_LEVEL, "Upgrading bank info save types.");
		BankLog.info(BankPluginConfiguration.BANK_LOG_PLUGIN_LEVEL, "Converting all saves, this may take some time");
		List<String> distinctUUIDS = upgradeInfoDatabase.getDistinctUUIDS();
		for(int i = 0; i < distinctUUIDS.size(); i++){
//		for(int i = 0; i < 10; i++){
			String uuid = distinctUUIDS.get(i);
			BankLog.info(BankPluginConfiguration.BANK_LOG_PLUGIN_LEVEL, "Upgrading " + uuid + ". " + i + "/" + distinctUUIDS.size());
			CorePlayers pl = new CorePlayers(uuid);
			BankPlayerManager.getInstance().addCorePlayers(pl);
			
			for(JSONInfo jsonInfo : pl.getAllInfoType(JSONInfo.class)){
				upgradeInfoDatabase.loadPlayer(pl, jsonInfo);
//				infoDatabase.getInfoTypeDatabase(jsonInfo).savePlayer(pl, jsonInfo, System.currentTimeMillis());

				if (jsonInfo instanceof BankItemsInfo) {
					BankItemsInfo itemsInfo = (BankItemsInfo) jsonInfo;
					boolean nonEmptyVault = false;

					for (Map.Entry<Integer, List<ItemStack>> itemMapTab : itemsInfo.getItemMap().entrySet()) {
						if (itemMapTab.getValue().size() > 0) {
							nonEmptyVault = true;
							break;
						}
					}

					if (nonEmptyVault) {
						BankPlugin plugin = BankPlugin.getInstance();
						plugin.itemVaultsToExportToPV.put(UUID.fromString(uuid), itemsInfo.getItemMap());
					}
				}
			}
		}
	}
}
