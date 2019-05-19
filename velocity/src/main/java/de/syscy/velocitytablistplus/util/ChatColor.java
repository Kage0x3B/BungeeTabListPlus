package de.syscy.velocitytablistplus.util;

import com.google.common.collect.Maps;
import net.kyori.text.format.TextColor;
import org.omg.CORBA.TCKind;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * All supported color values for chat
 */
public enum ChatColor {
	/**
	 * Represents black
	 */
	BLACK('0', 0x00, TextColor.BLACK),
	/**
	 * Represents dark blue
	 */
	DARK_BLUE('1', 0x1, TextColor.DARK_BLUE),
	/**
	 * Represents dark green
	 */
	DARK_GREEN('2', 0x2, TextColor.DARK_GREEN),
	/**
	 * Represents dark blue (aqua)
	 */
	DARK_AQUA('3', 0x3, TextColor.DARK_AQUA),
	/**
	 * Represents dark red
	 */
	DARK_RED('4', 0x4, TextColor.DARK_RED),
	/**
	 * Represents dark purple
	 */
	DARK_PURPLE('5', 0x5, TextColor.DARK_PURPLE),
	/**
	 * Represents gold
	 */
	GOLD('6', 0x6, TextColor.GOLD),
	/**
	 * Represents gray
	 */
	GRAY('7', 0x7, TextColor.GRAY),
	/**
	 * Represents dark gray
	 */
	DARK_GRAY('8', 0x8, TextColor.DARK_GRAY),
	/**
	 * Represents blue
	 */
	BLUE('9', 0x9, TextColor.BLUE),
	/**
	 * Represents green
	 */
	GREEN('a', 0xA, TextColor.GREEN),
	/**
	 * Represents aqua
	 */
	AQUA('b', 0xB, TextColor.AQUA),
	/**
	 * Represents red
	 */
	RED('c', 0xC, TextColor.RED),
	/**
	 * Represents light purple
	 */
	LIGHT_PURPLE('d', 0xD, TextColor.LIGHT_PURPLE),
	/**
	 * Represents yellow
	 */
	YELLOW('e', 0xE, TextColor.YELLOW),
	/**
	 * Represents white
	 */
	WHITE('f', 0xF, TextColor.WHITE),
	/**
	 * Represents magical characters that change around randomly
	 */
	MAGIC('k', 0x10, true, null),
	/**
	 * Makes the text bold.
	 */
	BOLD('l', 0x11, true, null),
	/**
	 * Makes a line appear through the text.
	 */
	STRIKETHROUGH('m', 0x12, true, null),
	/**
	 * Makes the text appear underlined.
	 */
	UNDERLINE('n', 0x13, true, null),
	/**
	 * Makes the text italic.
	 */
	ITALIC('o', 0x14, true, null),
	/**
	 * Resets all previous chat colors or formats.
	 */
	RESET('r', 0x15, null);

	/**
	 * The special character which prefixes all chat colour codes. Use this if
	 * you need to dynamically convert colour codes from your custom format.
	 */
	public static final char COLOR_CHAR = '\u00A7';
	private static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)" + COLOR_CHAR + "[0-9A-FK-OR]");

	private final int intCode;
	private final char code;
	private final boolean isFormat;
	private final String toString;
	private final TextColor textColor;

	private final static Map<Integer, ChatColor> BY_ID = Maps.newHashMap();
	private final static Map<Character, ChatColor> BY_CHAR = Maps.newHashMap();

	ChatColor(char code, int intCode, TextColor textColor) {
		this(code, intCode, false, textColor);
	}

	ChatColor(char code, int intCode, boolean isFormat, TextColor textColor) {
		this.code = code;
		this.intCode = intCode;
		this.isFormat = isFormat;
		this.toString = new String(new char[] { COLOR_CHAR, code });
		this.textColor = textColor;
	}

	/**
	 * Gets the char value associated with this color
	 *
	 * @return A char value of this color code
	 */
	public char getChar() {
		return code;
	}

	@Override
	public String toString() {
		return toString;
	}

	public String getName() {
		return name().toLowerCase();
	}

	public TextColor toTextColor() {
		return textColor;
	}

	/**
	 * Checks if this code is a format code as opposed to a color code.
	 */
	public boolean isFormat() {
		return isFormat;
	}

	/**
	 * Checks if this code is a color code as opposed to a format code.
	 */
	public boolean isColor() {
		return !isFormat && this != RESET;
	}

	/**
	 * Gets the color represented by the specified color code
	 *
	 * @param code Code to check
	 * @return Associative {@link ChatColor} with the given code,
	 *     or null if it doesn't exist
	 */
	public static ChatColor getByChar(char code) {
		return BY_CHAR.get(code);
	}

	/**
	 * Gets the color represented by the specified color code
	 *
	 * @param code Code to check
	 * @return Associative {@link ChatColor} with the given code,
	 *     or null if it doesn't exist
	 */
	public static ChatColor getByChar(String code) {
		return BY_CHAR.get(code.charAt(0));
	}

	/**
	 * Strips the given message of all color codes
	 *
	 * @param input String to strip of color
	 * @return A copy of the input string, without any coloring
	 */
	public static String stripColor(final String input) {
		if(input == null) {
			return null;
		}

		return STRIP_COLOR_PATTERN.matcher(input).replaceAll("");
	}

	/**
	 * Translates a string using an alternate color code character into a
	 * string that uses the internal ChatColor.COLOR_CODE color code
	 * character. The alternate color code character will only be replaced if
	 * it is immediately followed by 0-9, A-F, a-f, K-O, k-o, R or r.
	 *
	 * @param altColorChar The alternate color code character to replace. Ex: &
	 * @param textToTranslate Text containing the alternate color code character.
	 * @return Text containing the ChatColor.COLOR_CODE color code character.
	 */
	public static String translateAlternateColorCodes(char altColorChar, String textToTranslate) {
		char[] b = textToTranslate.toCharArray();
		for(int i = 0; i < b.length - 1; i++) {
			if(b[i] == altColorChar && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(b[i + 1]) > -1) {
				b[i] = ChatColor.COLOR_CHAR;
				b[i + 1] = Character.toLowerCase(b[i + 1]);
			}
		}
		return new String(b);
	}

	/**
	 * Gets the ChatColors used at the end of the given input string.
	 *
	 * @param input Input string to retrieve the colors from.
	 * @return Any remaining ChatColors to pass onto the next line.
	 */
	public static String getLastColors(String input) {
		String result = "";
		int length = input.length();

		// Search backwards from the end as it is faster
		for(int index = length - 1; index > -1; index--) {
			char section = input.charAt(index);
			if(section == COLOR_CHAR && index < length - 1) {
				char c = input.charAt(index + 1);
				ChatColor color = getByChar(c);

				if(color != null) {
					result = color.toString() + result;

					// Once we find a color or reset we can stop searching
					if(color.isColor() || color.equals(RESET)) {
						break;
					}
				}
			}
		}

		return result;
	}

	static {
		for(ChatColor color : values()) {
			BY_ID.put(color.intCode, color);
			BY_CHAR.put(color.code, color);
		}
	}}
