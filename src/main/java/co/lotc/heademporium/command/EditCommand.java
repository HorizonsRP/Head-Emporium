package co.lotc.heademporium.command;

import co.lotc.core.command.CommandTemplate;
import co.lotc.core.command.annotate.Cmd;

public class EditCommand extends BaseCommand {

	private final HeadCommand headCommand = new HeadCommand();
	private final CategoryCommand categoryCommand = new CategoryCommand();

	@Cmd(value="Opens the edit commands regarding categories within the shop.")
	public CommandTemplate categories() {
		return categoryCommand;
	}

	@Cmd(value="Opens the edit commands regarding heads within the shop.")
	public CommandTemplate heads() {
		return headCommand;
	}

}