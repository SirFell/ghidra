/* ###
 * IP: GHIDRA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ghidra.docking.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.*;
import java.util.Map.Entry;

import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.plaf.ComponentUI;

import ghidra.docking.util.painting.GRepaintManager;
import ghidra.framework.OperatingSystem;
import ghidra.framework.Platform;
import ghidra.framework.preferences.Preferences;
import ghidra.util.*;

import com.formdev.flatlaf.*;

/**
 * A utility class to manage LookAndFeel (LaF) settings.
 */
public class DockingWindowsLookAndFeelUtils {

	/**
	 * Preference name for look and feel for the application.
	 */
	public final static String LAST_LOOK_AND_FEEL_KEY = "LastLookAndFeel";

	/**
	 * Preference name for whether to use inverted colors.
	 */
	public final static String USE_INVERTED_COLORS_KEY = "LookAndFeel.UseInvertedColors";

	/**
	 * Metal is the non-system, generic Java Look and Feel.
	 */
	public final static String METAL_LOOK_AND_FEEL = "Metal";

	/**
	 * Default Look and feel for the current platform.
	 */
	private final static String SYSTEM_LOOK_AND_FEEL = "System";

	/**
	 * The most stable Linux LaF, based on anecdotal observation.
	 */
	private static final String NIMBUS_LOOK_AND_FEEL = "Nimbus";

	/**
	 * The flatlaf implementation of light mode.
	 */
	private static final String FLAT_LIGHT_LOOK_AND_FEEL = "Flat Light";

	/**
	 * The flatlaf implementation of dark mode.
	 */
	public static final String FLAT_DARK_LOOK_AND_FEEL = "Flat Dark";

  /**
	 * The Motif LaF name.
	 */
	private static final String MOTIF_LOOK_AND_FEEL = "CDE/Motif";

	private static RepaintManager defaultSwingRepaintManager = null;

	private DockingWindowsLookAndFeelUtils() {
		// utils class, cannot create
	}

	/**
	 * Loads settings from {@link Preferences}.
	 */
	public static void loadFromPreferences() {

		boolean useHistoricalValue = true;
		String laf = Preferences.getProperty(LAST_LOOK_AND_FEEL_KEY, getDefaultLookAndFeelName(),
			useHistoricalValue);
		setLookAndFeel(laf);

		boolean useInvertedColors = getUseInvertedColorsPreference();
		setUseInvertedColors(useInvertedColors);

		//
		// Users can change this via the SystemUtilities.FONT_SIZE_OVERRIDE_PROPERTY_NAME
		// system property.
		//
		Integer fontOverride = SystemUtilities.getFontSizeOverrideValue();
		if (fontOverride != null) {
			setGlobalFontSizeOverride(fontOverride);
		}
	}

	/**
	 * Returns the {@link Preferences} value for whether to use inverted colors when painting.
	 * @return the {@link Preferences} value for whether to use inverted colors when painting.
	 */
	public static boolean getUseInvertedColorsPreference() {
		boolean useHistoricalValue = true;
		String useInvertedColorsString = Preferences.getProperty(USE_INVERTED_COLORS_KEY,
			Boolean.FALSE.toString(), useHistoricalValue);
		boolean useInvertedColors = Boolean.parseBoolean(useInvertedColorsString);
		return useInvertedColors;
	}

	/**
	 * Returns the currently installed LaF.
	 * @return the currently installed LaF.
	 */
	public static String getInstalledLookAndFeelName() {
		return UIManager.getLookAndFeel().getName();
	}

	/**
	 * Set the look and feel (LAF) indicated by the string passed in as a parameter.
	 * The string value can be either the class name of the LAF, as returned by
	 * <code>LookAndFeelInfo.getClassName()</code> or the name as returned by
	 * <code>LookAndFeelInfo.getName()</code>.
	 * <p>
	 * Note: to be effective, this call needs to be made before any components have been created
	 * and shown.
	 * 
	 * @param lookAndFeelName the string indicating which look and feel is desired (see above)
	 */
	public static void setLookAndFeel(String lookAndFeelName) {
		SystemUtilities.runSwingNow(() -> {
			try {
				installLookAndFeelByName(lookAndFeelName);

				// some custom values for any given LAF
				installGlobalLookAndFeelAttributes();
				installGlobalFontSizeOverride();
				installCustomLookAndFeelActions();
				installPopupMenuSettingsOverride();
			}
			catch (Exception exc) {
				Msg.error(DockingWindowsLookAndFeelUtils.class,
					"Error loading Look and Feel: " + exc, exc);
			}
		});
	}

	/**
	 * Returns all installed LaFs.  This will vary by OS.
	 * @return all installed LaFs.
	 */
	public static List<String> getLookAndFeelNames() {
		List<String> list = new ArrayList<>();
		list.add(DockingWindowsLookAndFeelUtils.SYSTEM_LOOK_AND_FEEL);
		list.add(DockingWindowsLookAndFeelUtils.FLAT_LIGHT_LOOK_AND_FEEL);
		list.add(DockingWindowsLookAndFeelUtils.FLAT_DARK_LOOK_AND_FEEL);

		LookAndFeelInfo[] installedLookAndFeels = UIManager.getInstalledLookAndFeels();
		for (LookAndFeelInfo info : installedLookAndFeels) {
			list.add(info.getName());
		}
		return list;
	}

	private static void installLookAndFeelByName(String lookAndFeelName)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException,
			UnsupportedLookAndFeelException {

		String lookAndFeelClassName = findLookAndFeelClassName(lookAndFeelName);
		UIManager.setLookAndFeel(lookAndFeelClassName);
		fixupLookAndFeelIssues();
	}

	private static String findLookAndFeelClassName(String lookAndFeelName) {
		if (lookAndFeelName.equalsIgnoreCase(SYSTEM_LOOK_AND_FEEL)) {
			return UIManager.getSystemLookAndFeelClassName();
		} else if (lookAndFeelName.equalsIgnoreCase(FLAT_LIGHT_LOOK_AND_FEEL)) {
			return "com.formdev.flatlaf.FlatLightLaf";
		} else if (lookAndFeelName.equalsIgnoreCase(FLAT_DARK_LOOK_AND_FEEL)) {
			return "com.formdev.flatlaf.FlatDarkLaf";
		}

		LookAndFeelInfo[] installedLookAndFeels = UIManager.getInstalledLookAndFeels();
		for (LookAndFeelInfo info : installedLookAndFeels) {
			String className = info.getClassName();
			if (lookAndFeelName.equals(className) || lookAndFeelName.equals(info.getName())) {
				return className;
			}
		}

		Msg.debug(DockingWindowsLookAndFeelUtils.class,
			"Unable to find requested Look and Feel: " + lookAndFeelName);
		return UIManager.getSystemLookAndFeelClassName();
	}

	public static void setUseInvertedColors(boolean useInvertedColors) {
		SystemUtilities.runIfSwingOrPostSwingLater(() -> {

			if (defaultSwingRepaintManager == null) {
				defaultSwingRepaintManager = RepaintManager.currentManager(null /*unused*/);
			}

			RepaintManager rm = defaultSwingRepaintManager;
			if (useInvertedColors && !isUsingFlatUI()) {
				rm = new GRepaintManager();
			}
			RepaintManager.setCurrentManager(rm);
		});
	}

	/**
	 * Fixes issues in the currently running look and feel.
	 */
	private static void fixupLookAndFeelIssues() {
		LookAndFeel lookAndFeel = UIManager.getLookAndFeel();
		switch (lookAndFeel.getName()) {
			case NIMBUS_LOOK_AND_FEEL:
				// fix scroll bar grabber disappearing.  See https://bugs.openjdk.java.net/browse/JDK-8134828
				// This fix looks like it should not cause harm even if the bug is fixed on the jdk side.
				UIDefaults defaults = UIManager.getDefaults();
				defaults.put("ScrollBar.minimumThumbSize", new Dimension(30, 30));

				// (see NimbusDefaults for key values that can be changed here)
				break;
			case MOTIF_LOOK_AND_FEEL:

				doFixupMissingCopyPasteKeyBindings();

				// (see MotifDefaults for key values that can be changed here)
				break;
		}
	}

	private static void doFixupMissingCopyPasteKeyBindings() {

		//
		// The Motif LaF does not bind copy/paste/cut to Control-C/V/X by default.  Rather, they
		// only use the COPY/PASTE/CUT keys.  The other LaFs bind both shortcuts.
		//

		// these prefixes are for text components
		String[] UIPrefixValues =
			{ "TextField", "FormattedTextField", "TextArea", "TextPane", "EditorPane" };

		setKeyBinding("COPY", "ctrl C", UIPrefixValues);
		setKeyBinding("PASTE", "ctrl V", UIPrefixValues);
		setKeyBinding("CUT", "ctrl X", UIPrefixValues);
	}

	private static void installGlobalLookAndFeelAttributes() {
		// Fix up the default fonts that Java 1.5.0 changed to Courier, which looked terrible.
		Font f = new Font("Monospaced", Font.PLAIN, 12);
		UIManager.put("PasswordField.font", f);
		UIManager.put("TextArea.font", f);

		// We like buttons that change on hover, so force that to happen (see Tracker SCR 3966)
		UIManager.put("Button.rollover", Boolean.TRUE);
		UIManager.put("ToolBar.isRollover", Boolean.TRUE);
	}

	private static void installPopupMenuSettingsOverride() {
		// Java 1.6 UI consumes MousePressed event when dismissing popup menu
		// which prevents application components from getting this event.
		UIManager.put("PopupMenu.consumeEventOnClose", Boolean.FALSE);
	}

	private static void installGlobalFontSizeOverride() {

		// only set a global size if the property is set
		Integer overrideFontInteger = SystemUtilities.getFontSizeOverrideValue();
		if (overrideFontInteger == null) {
			return;
		}

		setGlobalFontSizeOverride(overrideFontInteger);
	}

	private static void installCustomLookAndFeelActions() {
		// these prefixes are for text components
		String[] UIPrefixValues =
			{ "TextField", "FormattedTextField", "TextArea", "TextPane", "EditorPane" };

		DeleteToStartOfWordAction deleteToStartOfWordAction = new DeleteToStartOfWordAction();
		registerAction(deleteToStartOfWordAction, DeleteToStartOfWordAction.KEY_STROKE,
			UIPrefixValues);

		DeleteToEndOfWordAction deleteToEndOfWordAction = new DeleteToEndOfWordAction();
		registerAction(deleteToEndOfWordAction, DeleteToEndOfWordAction.KEY_STROKE, UIPrefixValues);

		BeginningOfLineAction beginningOfLineAction = new BeginningOfLineAction();
		registerAction(beginningOfLineAction, BeginningOfLineAction.KEY_STROKE, UIPrefixValues);

		EndOfLineAction endOfLineAction = new EndOfLineAction();
		registerAction(endOfLineAction, EndOfLineAction.KEY_STROKE, UIPrefixValues);

		SelectBeginningOfLineAction selectBeginningOfLineAction = new SelectBeginningOfLineAction();
		registerAction(selectBeginningOfLineAction, SelectBeginningOfLineAction.KEY_STROKE,
			UIPrefixValues);

		SelectEndOfLineAction selectEndOfLineAction = new SelectEndOfLineAction();
		registerAction(selectEndOfLineAction, SelectEndOfLineAction.KEY_STROKE, UIPrefixValues);
	}

	private static void registerAction(Action action, KeyStroke keyStroke, String[] prefixValues) {
		for (String properyPrefix : prefixValues) {
			UIDefaults defaults = UIManager.getDefaults();
			Object object = defaults.get(properyPrefix + ".focusInputMap");
			InputMap inputMap = (InputMap) object;
			inputMap.put(keyStroke, action);
		}
	}

	private static void setKeyBinding(String existingKsText, String newKsText,
			String[] prefixValues) {

		KeyStroke existingKs = KeyStroke.getKeyStroke(existingKsText);
		KeyStroke newKs = KeyStroke.getKeyStroke(newKsText);

		for (String properyPrefix : prefixValues) {

			UIDefaults defaults = UIManager.getDefaults();
			Object object = defaults.get(properyPrefix + ".focusInputMap");
			InputMap inputMap = (InputMap) object;
			Object action = inputMap.get(existingKs);
			inputMap.put(newKs, action);
		}
	}

	/** Allows you to globally set the font size (don't use this method!) */
	private static void setGlobalFontSizeOverride(int fontSize) {
		UIDefaults defaults = UIManager.getDefaults();

		Set<Entry<Object, Object>> set = defaults.entrySet();
		Iterator<Entry<Object, Object>> iterator = set.iterator();
		while (iterator.hasNext()) {
			Entry<Object, Object> entry = iterator.next();
			Object key = entry.getKey();

			if (key.toString().toLowerCase().indexOf("font") != -1) {
				Font currentFont = defaults.getFont(key);
				if (currentFont != null) {
					Font newFont = currentFont.deriveFont((float) fontSize);
					UIManager.put(key, newFont);
				}
			}
		}
	}

	/**
	 * Returns the name of the default LookAndFeel for the current OS.
	 */
	private static String getDefaultLookAndFeelName() {
		OperatingSystem OS = Platform.CURRENT_PLATFORM.getOperatingSystem();
		if (OS == OperatingSystem.LINUX) {
			return NIMBUS_LOOK_AND_FEEL;
		}
		return SYSTEM_LOOK_AND_FEEL;
	}

	/**
	 * Returns true if the given UI object is using the Aqua Look and Feel.
	 * @param UI the UI to examine.
	 * @return true if the UI is using Aqua
	 */
	public static boolean isUsingAquaUI(ComponentUI UI) {
		Class<? extends ComponentUI> clazz = UI.getClass();
		String name = clazz.getSimpleName();
		return name.startsWith("Aqua");
	}

	/**
	 * Returns true if 'Nimbus' is the current Look and Feel
	 * @return true if 'Nimbus' is the current Look and Feel
	 */
	public static boolean isUsingNimbusUI() {
		LookAndFeel lookAndFeel = UIManager.getLookAndFeel();
		return NIMBUS_LOOK_AND_FEEL.equals(lookAndFeel.getName());
	}


	/**
	 * Returns true if 'Flat' is the current Look and Feel
	 * @return true if 'Flat' is the current Look and Feel
	 */
	public static boolean isUsingFlatUI() {
		return getInstalledLookAndFeelName().startsWith("FlatLaf");
	}

	/**
	 * Returns true if 'Flat Dark' is the current Look and Feel
	 * @return true if 'Flat Dark' is the current Look and Feel
	 */
	public static boolean isUsingFlatDarkUI() {
		return getInstalledLookAndFeelName().startsWith("FlatLaf Dark");
  }

	public static boolean isUsingMotifUI() {
		LookAndFeel lookAndFeel = UIManager.getLookAndFeel();
		return MOTIF_LOOK_AND_FEEL.equals(lookAndFeel.getName());
	}
}
