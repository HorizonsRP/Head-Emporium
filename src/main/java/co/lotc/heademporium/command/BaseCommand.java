package co.lotc.heademporium.command;

import co.lotc.core.command.CommandTemplate;
import co.lotc.heademporium.HeadEmporium;

public class BaseCommand extends CommandTemplate {
	protected final HeadEmporium plugin = HeadEmporium.get();
}