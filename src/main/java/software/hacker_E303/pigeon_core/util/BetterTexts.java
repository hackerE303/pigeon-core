package software.hacker_E303.pigeon_core.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

/**
 * Utility methods for string case conversion and styled text component creation.
 */
public class BetterTexts {

    /**
     * Converts a camelCase string to snake_case.
     *
     * @param camel the camelCase input
     * @return the snake_case result
     */
    public static String camelToSnake(String camel) {
        return camel.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
    }

    /**
     * Converts a snake_case string to CamelCase.
     *
     * @param snake the snake_case input
     * @return the CamelCase result
     */
    public static String snakeToCamel(String snake) {
		
        StringBuilder sb = new StringBuilder();
        for (String part : snake.split("_")) {

            sb.append(Character.toUpperCase(part.charAt(0)))
				.append(part.substring(1));
        }
        return sb.toString();
    }

    /**
     * Creates a colored text component from a raw string.
     *
     * @param input the text to color
     * @param rgb   the packed RGB color value
     * @return the colored {@link MutableComponent}
     */
	public static MutableComponent colorText(String input, int rgb) {
		return Component.literal(input).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)));
	}

    /**
     * Parses a string containing hex color codes (&#RRGGBB) and builds a
     * gradient-like {@link MutableComponent} with italic formatting.
     *
     * @param input the formatted text containing color codes
     * @return the assembled component, or a plain literal if no codes were found
     */
	public static MutableComponent gradientColorText(String input) {

     	Pattern pattern = Pattern.compile("&#([A-Fa-f0-9]{6})([^&]*)");
     	Matcher matcher = pattern.matcher(input);
     
     	MutableComponent finalComponent = Component.empty();
     	boolean found = false;

     	while (matcher.find()) {
         	found = true;

         	String hexColor = "#" + matcher.group(1);
         	String text = matcher.group(2);

         	Style style = Style.EMPTY.withColor(TextColor.parseColor(hexColor));
         	style = style.withItalic(true);

         	finalComponent.append(Component.literal(text).withStyle(style));
     	}
     	return found ? finalComponent : Component.literal(input);
	}

    /**
     * Converts an underscore-separated string to title case.
     *
     * @param input the underscore-separated input
     * @return the title-cased result
     */
    public static String titleCase(String input) {
        StringBuilder sb  = new StringBuilder();
        boolean       up  = true;
        for (char c : input.replace('_', ' ').toCharArray()) {
            sb.append(up ? Character.toUpperCase(c) : c);
            up = c == ' ';
        }
        return sb.toString();
    }
}