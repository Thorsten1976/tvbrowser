/*
 * Created on 24.09.2004
 */
package tvbrowser.core.plugin;

import java.awt.Frame;
import java.io.File;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import bsh.Interpreter;

import tvbrowser.core.plugin.beanshell.BeanShellScriptIf;
import util.exc.TvBrowserException;
import devplugin.ChannelDayProgram;
import devplugin.PluginInfo;
import devplugin.Program;
import devplugin.SettingsTab;

/**
 * @author bodum
 */
public class BeanShellPluginProxy extends AbstractPluginProxy {

    private static final ImageIcon BEANICON = new ImageIcon("imgs/beanshell.png");

    private File mBshFile;

    private BeanShellScriptIf mScript;

    public BeanShellPluginProxy(File bshFile) {
        mBshFile = bshFile;
        loadScript(mBshFile);
    }

    /**
     * @param bshFile
     */
    private void loadScript(File bshFile) {
        try {
            mScript = (BeanShellScriptIf) new Interpreter().source(bshFile.getAbsolutePath());
        } catch (Exception e) {
            mScript = null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see tvbrowser.core.plugin.AbstractPluginProxy#setParentFrame(java.awt.Frame)
     */
    void setParentFrame(Frame parent) {

        if (mScript == null) { return; }

        try {
            mScript.setParentFrame(parent);
        } catch (Exception e) {
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see tvbrowser.core.plugin.AbstractPluginProxy#doLoadSettings(java.io.File)
     */
    protected void doLoadSettings(File userDirectory) throws TvBrowserException {
    }

    /*
     * (non-Javadoc)
     * 
     * @see tvbrowser.core.plugin.AbstractPluginProxy#doSaveSettings(java.io.File)
     */
    protected void doSaveSettings(File userDirectory) throws TvBrowserException {
    }

    /*
     * (non-Javadoc)
     * 
     * @see tvbrowser.core.plugin.AbstractPluginProxy#doGetInfo()
     */
    protected PluginInfo doGetInfo() {
        if (mScript != null) {
            try {
                return mScript.getInfo();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new PluginInfo(mBshFile.getName());
    }

    /*
     * (non-Javadoc)
     * 
     * @see tvbrowser.core.plugin.AbstractPluginProxy#doCanReceivePrograms()
     */
    protected boolean doCanReceivePrograms() {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see tvbrowser.core.plugin.AbstractPluginProxy#doReceivePrograms(devplugin.Program[])
     */
    protected void doReceivePrograms(Program[] programArr) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see tvbrowser.core.plugin.AbstractPluginProxy#doGetSettingsTab()
     */
    protected SettingsTab doGetSettingsTab() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see tvbrowser.core.plugin.AbstractPluginProxy#goGetContextMenuActions(devplugin.Program)
     */
    protected Action[] goGetContextMenuActions(Program program) {
        if (mScript == null) { return null; }

        try {
            Action[] actions = mScript.getContextMenuActions(program);

            for (int i = 0; i < actions.length; i++) {
                if (actions[i].getValue(Action.SMALL_ICON) == null) {
                    actions[i].putValue(Action.SMALL_ICON, BEANICON);
                }

                if (actions[i].getValue(Action.NAME) == null) {
                    actions[i].putValue(Action.NAME, mBshFile.getName());
                }

            }

            return actions;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see tvbrowser.core.plugin.AbstractPluginProxy#doGetButtonAction()
     */
    protected Action doGetButtonAction() {
        if (mScript == null) { return null; }

        try {
            Action action = mScript.getButtonAction();

            if (action.getValue(Action.SMALL_ICON) == null) {
                action.putValue(Action.SMALL_ICON, BEANICON);
            }

            if (action.getValue(Action.NAME) == null) {
                action.putValue(Action.NAME, mBshFile.getName());
            }

            return action;
        } catch (Exception e) {
        }

        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see tvbrowser.core.plugin.AbstractPluginProxy#doGetMarkIcon()
     */
    protected Icon doGetMarkIcon() {
        return BEANICON;
    }

    /*
     * (non-Javadoc)
     * 
     * @see tvbrowser.core.plugin.AbstractPluginProxy#doGetProgramTableIconText()
     */
    protected String doGetProgramTableIconText() {
        if (mScript != null) {
            try {
                return mScript.getProgramTableIconText();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see tvbrowser.core.plugin.AbstractPluginProxy#doGetProgramTableIcons(devplugin.Program)
     */
    protected Icon[] doGetProgramTableIcons(Program program) {
        if (mScript != null) {
            try {
                return mScript.getProgramTableIcons(program);
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see tvbrowser.core.plugin.AbstractPluginProxy#doHandleTvDataUpdateFinished()
     */
    protected void doHandleTvDataUpdateFinished() {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see tvbrowser.core.plugin.AbstractPluginProxy#doHandleTvDataAdded(devplugin.ChannelDayProgram)
     */
    protected void doHandleTvDataAdded(ChannelDayProgram newProg) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see tvbrowser.core.plugin.AbstractPluginProxy#doHandleTvDataDeleted(devplugin.ChannelDayProgram)
     */
    protected void doHandleTvDataDeleted(ChannelDayProgram oldProg) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see devplugin.PluginAccess#getId()
     */
    public String getId() {
        return "bsh." + mBshFile.getName();
    }

}