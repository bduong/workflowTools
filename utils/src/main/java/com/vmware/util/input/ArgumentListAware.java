package com.vmware.util.input;

import jline.console.completer.ArgumentCompleter;

public interface ArgumentListAware {

    public void setArgumentList(ArgumentCompleter.ArgumentList argumentList);
}
