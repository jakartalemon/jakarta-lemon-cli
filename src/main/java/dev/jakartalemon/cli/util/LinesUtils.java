package dev.jakartalemon.cli.util;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

import static dev.jakartalemon.cli.util.Constants.COMMA;

public class LinesUtils {

    private LinesUtils(){

    }

    public static void removeLastComma(List<String> lines){
        var lastLine = StringUtils.substringBeforeLast(lines.get(lines.size() - 1),COMMA);
        lines.set(lines.size() - 1, lastLine);
    }
}
