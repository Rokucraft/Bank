package me.dablakbandit.bank.command.arguments.admin.item;

import me.dablakbandit.bank.config.BankLanguageConfiguration;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.dablakbandit.bank.command.base.BankEndArgument;
import me.dablakbandit.bank.config.BankPluginConfiguration;
import me.dablakbandit.bank.inventory.BankInventories;
import me.dablakbandit.bank.inventory.BankInventoriesManager;
import me.dablakbandit.core.command.config.CommandConfiguration;
import me.dablakbandit.core.players.CorePlayerManager;

public class BlacklistArgument extends BankEndArgument{
	
	public BlacklistArgument(CommandConfiguration.Command command){
		super(command);
	}
	
	@Override
	protected void onArgument(CommandSender s, Command cmd, String label, String[] args, String[] original){
		BankInventoriesManager.getInstance().openBypass(CorePlayerManager.getInstance().getPlayer((Player)s), BankInventories.BANK_ADMIN_BLACKLIST);
	}

}
