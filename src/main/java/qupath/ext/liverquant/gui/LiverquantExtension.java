package qupath.ext.liverquant.gui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.common.Version;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.extensions.GitHubProject;
import qupath.lib.gui.extensions.QuPathExtension;
import qupath.lib.gui.tools.MenuTools;

import java.util.ResourceBundle;

/**
 * A QuPath extension to assess Non-alcoholic Fatty Liver Disease (NAFLD).
 */
public class LiverquantExtension implements QuPathExtension, GitHubProject {
	
	private static final Logger logger = LoggerFactory.getLogger(LiverquantExtension.class);
	private static final ResourceBundle resources = UiUtilities.getResources();
	private static final String EXTENSION_NAME = resources.getString("LiverquantExtension.name");
	private static final String EXTENSION_DESCRIPTION = resources.getString("LiverquantExtension.description");
	private static final Version EXTENSION_QUPATH_VERSION = Version.parse("v0.5.0");
	private static final GitHubRepo EXTENSION_REPOSITORY = GitHubRepo.create(EXTENSION_NAME, "qupath", "qupath-extension-liverquant");
	private boolean isInstalled = false;

	@Override
	public void installExtension(QuPathGUI qupath) {
		if (isInstalled) {
			logger.debug("{} is already installed", getName());
		} else {
			isInstalled = true;

			MenuTools.addMenuItems(
					qupath.getMenu("Extensions", false),
					MenuTools.createMenu("Liverquant", new DetectFatGlobulesMenu(qupath))
			);
		}
	}

	@Override
	public String getName() {
		return EXTENSION_NAME;
	}

	@Override
	public String getDescription() {
		return EXTENSION_DESCRIPTION;
	}

	@Override
	public Version getQuPathVersion() {
		return EXTENSION_QUPATH_VERSION;
	}

	@Override
	public GitHubRepo getRepository() {
		return EXTENSION_REPOSITORY;
	}
}
