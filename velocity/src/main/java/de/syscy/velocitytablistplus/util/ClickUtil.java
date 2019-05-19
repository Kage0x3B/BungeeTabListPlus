package de.syscy.velocitytablistplus.util;

import lombok.experimental.UtilityClass;
import net.kyori.text.event.ClickEvent;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;
import java.util.StringJoiner;

@UtilityClass
public class ClickUtil {
  /**
   * Creates a click event that opens a url.
   *
   * @param url the url to open
   * @return a click event
   */
  public static @NonNull ClickEvent openUrl(final @NonNull String url) {
    return new ClickEvent(ClickEvent.Action.OPEN_URL, url);
  }

  /**
   * Creates a click event that opens a file.
   *
   * <p>This action is not readable, and may only be used locally on the client.</p>
   *
   * @param file the file to open
   * @return a click event
   */
  public static @NonNull ClickEvent openFile(final @NonNull String file) {
    return new ClickEvent(ClickEvent.Action.OPEN_FILE, file);
  }

  /**
   * Creates a click event that runs a command.
   *
   * @param command the command to run
   * @return a click event
   */
  public static @NonNull ClickEvent runCommand(final @NonNull String command) {
    return new ClickEvent(ClickEvent.Action.RUN_COMMAND, command);
  }

  /**
   * Creates a click event that suggests a command.
   *
   * @param command the command to suggest
   * @return a click event
   */
  public static @NonNull ClickEvent suggestCommand(final @NonNull String command) {
    return new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command);
  }

  /**
   * Creates a click event that changes to a page.
   *
   * @param page the page to change to
   * @return a click event
   */
  public static @NonNull ClickEvent changePage(final int page) {
    return changePage(String.valueOf(page));
  }

  /**
   * Creates a click event that changes to a page.
   *
   * @param page the page to change to
   * @return a click event
   */
  public static @NonNull ClickEvent changePage(final @NonNull String page) {
    return new ClickEvent(ClickEvent.Action.CHANGE_PAGE, page);
  }
}