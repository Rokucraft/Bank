package me.dablakbandit.bank.implementations.placeholder;

import org.bukkit.entity.Player;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.dablakbandit.bank.BankPlugin;
import me.dablakbandit.bank.config.BankPluginConfiguration;
import me.dablakbandit.bank.implementations.BankImplementation;
import me.dablakbandit.bank.log.BankLog;
import me.dablakbandit.bank.player.info.BankExpInfo;
import me.dablakbandit.bank.player.info.BankItemsInfo;
import me.dablakbandit.bank.player.info.BankMoneyInfo;
import me.dablakbandit.bank.utils.format.Format;
import me.dablakbandit.core.players.CorePlayerManager;
import me.dablakbandit.core.players.CorePlayers;

@SuppressWarnings("deprecation")
public class PlaceholderAPIImplementation extends BankImplementation{
	
	private static final PlaceholderAPIImplementation manager = new PlaceholderAPIImplementation();
	
	public static PlaceholderAPIImplementation getInstance(){
		return manager;
	}
	
	private final BankPlaceHolderExpansion expansion = new BankPlaceHolderExpansion();
	
	private PlaceholderAPIImplementation(){
	}
	
	@Override
	public void load(){
		
	}
	
	@Override
	public void enable(){
		BankLog.info(BankPluginConfiguration.BANK_LOG_PLUGIN_LEVEL, "Enabled placeholderapi expansion");
		expansion.register();
	}
	
	@Override
	public void disable(){
		
	}
	
	public static class BankPlaceHolderExpansion extends PlaceholderExpansion{
		
		@Override
		public boolean persist(){
			return true;
		}
		
		@Override
		public boolean canRegister(){
			return true;
		}
		
		@Override
		public String getIdentifier(){
			return "bank";
		}
		
		@Override
		public String getAuthor(){
			return "Dablakbandit";
		}
		
		@Override
		public String getVersion(){
			return BankPlugin.getInstance().getDescription().getVersion();
		}
		
		@Override
		public String onPlaceholderRequest(Player player, String holder){
			CorePlayers pl = CorePlayerManager.getInstance().getPlayer(player);
			if(pl == null){ return null; }
			switch(holder){
			case "money":{
				BankMoneyInfo info = pl.getInfo(BankMoneyInfo.class);
				if(info == null){ return ""; }
				return Format.formatMoney(info.getMoney());
			}
			case "exp":{
				BankExpInfo info = pl.getInfo(BankExpInfo.class);
				if(info == null){ return ""; }
				return Format.formatMoney(info.getExp());
			}
			case "slots":{
				BankItemsInfo info = pl.getInfo(BankItemsInfo.class);
				if(info == null){ return ""; }
				return "" + info.getTotalSlots();
			}
			case "used_slots":{
				BankItemsInfo info = pl.getInfo(BankItemsInfo.class);
				if(info == null){ return ""; }
				return "" + info.getTotalUsedSlots();
			}
			}
			return null;
		}
	}
}
