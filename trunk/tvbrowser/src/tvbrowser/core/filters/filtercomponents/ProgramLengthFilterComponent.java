/*
 * Created on 09.04.2004
 */
package tvbrowser.core.filters.filtercomponents;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import tvbrowser.core.filters.FilterComponent;
import devplugin.Program;


/**
 * Dieser Filter akzeptiert Sendungen mit selbst definiertbaren min und max L�ngen 
 * 
 * @author bodo
 */
public class ProgramLengthFilterComponent implements FilterComponent {

	/**
	 * Erzeugt einen leeren Filter
     */
    public ProgramLengthFilterComponent() {
        this("", "");
    }

    /**
     * Erzeugt einen Filter
     * @param name Name 
     * @param description Beschreibung
     */
    public ProgramLengthFilterComponent(String name, String description) {
        _name = name;
        _desc = description;
    }    
    
    /**
     * Gibt die Version zur�ck
     * @see tvbrowser.core.filters.FilterComponent#getVersion()
     */
    public int getVersion() {
        return 1;
    }

    /**
     * Aktzeptiert nur Sendungen einer bestimmten L�nge
     * @see tvbrowser.core.filters.FilterComponent#accept(devplugin.Program)
     */
    public boolean accept(Program program) {
        
        if (_useMin && (program.getLength() < _min)) {
    	        return false;
        }

        if (_useMax && (program.getLength() > _max)) {
    	        return false;
        }
        
        return true;
    }

    /**
     * Liest die Einstellungen
     * @see tvbrowser.core.filters.FilterComponent#read(java.io.ObjectInputStream, int)
     */
    public void read(ObjectInputStream in, int version) throws IOException,
            ClassNotFoundException {
		_useMin = in.readBoolean();
		_useMax = in.readBoolean();
		_min = in.readInt();
		_max = in.readInt();
    }

    /**
     * Schreibt die Einstellungen
     * @see tvbrowser.core.filters.FilterComponent#write(java.io.ObjectOutputStream)
     */
    public void write(ObjectOutputStream out) throws IOException {
		out.writeBoolean(_useMin);
		out.writeBoolean(_useMax);
		out.writeInt(_min);
		out.writeInt(_max);
    }

    /** 
     * Erzeugt das Settings-Panel
     * @see tvbrowser.core.filters.FilterComponent#getPanel()
     */
    public JPanel getPanel() {
    	JPanel panel = new JPanel();

		_minSpinner = new JSpinner(new SpinnerNumberModel(_min, 0, 1000, 1));;
		_maxSpinner = new JSpinner(new SpinnerNumberModel(_max, 0, 1000, 1));;
		_minBox = new JCheckBox("minimum", _useMin);
		_maxBox = new JCheckBox("maximum", _useMax);

		final JLabel minMinutes = new JLabel("Minutes");
		final JLabel maxMinutes = new JLabel("Minutes");
		
		if (!_useMin) {
            _minSpinner.setEnabled(false);
            minMinutes.setEnabled(false);
		}

		if (!_useMax) {
            _maxSpinner.setEnabled(false);
            maxMinutes.setEnabled(false);
		}
		
		_minBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _minSpinner.setEnabled(_minBox.isSelected());
                minMinutes.setEnabled(_minBox.isSelected());
            }
		});
		_maxBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _maxSpinner.setEnabled(_maxBox.isSelected());
                maxMinutes.setEnabled(_minBox.isSelected());
            }
		});
		
    	panel.setLayout(new GridBagLayout());
    	
        GridBagConstraints a = new GridBagConstraints();
        a.gridwidth = GridBagConstraints.REMAINDER;
        a.fill = GridBagConstraints.HORIZONTAL;
		a.weightx = 0.7;

        GridBagConstraints b = new GridBagConstraints();
        b.fill = GridBagConstraints.NONE;
		
       _minSpinner.setEditor(new JSpinner.NumberEditor(_minSpinner, "###0"));
       _maxSpinner.setEditor(new JSpinner.NumberEditor(_maxSpinner, "###0"));

		panel.add(_minBox, b);
		panel.add(_minSpinner, b);    	
		panel.add(minMinutes, a);    	

		panel.add(_maxBox, b);    	
		panel.add(_maxSpinner, b);    	
		panel.add(maxMinutes, a);    	
    
        return panel;
    }


    /**
     * Schreibt die GUI-Daten in die Variablen
     * @see tvbrowser.core.filters.FilterComponent#ok()
     */
    public void ok() {
        _min = ((Integer)_minSpinner.getValue()).intValue();
        _max = ((Integer)_maxSpinner.getValue()).intValue();
        _useMin = _minBox.isSelected();
        _useMax = _maxBox.isSelected();
    }

    /**
     * Gibt den momentanen Namen des Filters zur�ck
     * @see tvbrowser.core.filters.FilterComponent#getName()
     */
    public String getName() {
        return _name;
    }

    /**
     * Gibt die momentane Beschreibung des Filters zur�ck
     * @see tvbrowser.core.filters.FilterComponent#getDescription()
     */
    public String getDescription() {
        return _desc;
    }

    /**
     * Setzt den Namen des Filters
     * @see tvbrowser.core.filters.FilterComponent#setName(java.lang.String)
     */
    public void setName(String name) {
        _name = name;
    }

    /**
     * Setzt die Beschreibung des Filters
     * @see tvbrowser.core.filters.FilterComponent#setDescription(java.lang.String)
     */
    public void setDescription(String desc) {
        _desc = desc;
    }

    /**
     * Gibt den Namen des Filters zur�ck 
     */
    public String toString() {
        return mLocalizer.msg("ProgrammLength", "Program length");
    }

    /**
     * Der Lokalizer
     */
    private static final util.ui.Localizer mLocalizer = util.ui.Localizer.getLocalizerFor(ProgramLengthFilterComponent.class);    
    
    /**
     * Name des Filters
     */
    private String _name;

    /**
     * Beschreibung des Filters
     */
    private String _desc;

    /** Minimal-L�nge */
	private int _min;
	/** Maximal-L�nge */
	private int _max;
	/** Minimum benutzen? */
	private boolean _useMin;
	/** Maximum benutzen */
	private boolean _useMax;
	
	/** GUI-Komponenten f�r das Panel */
	private JSpinner _minSpinner;
	private JSpinner _maxSpinner;
	private JCheckBox _minBox;
	private JCheckBox _maxBox;
}