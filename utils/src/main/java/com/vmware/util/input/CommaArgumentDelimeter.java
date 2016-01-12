package com.vmware.util.input;

import jline.console.completer.ArgumentCompleter;

public class CommaArgumentDelimeter extends ArgumentCompleter.WhitespaceArgumentDelimiter {
    @Override
    public boolean isDelimiterChar(CharSequence buffer, int pos) {
        return buffer.charAt(pos) == ',';
    }
}
