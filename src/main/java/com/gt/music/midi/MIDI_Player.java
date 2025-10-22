//
// MIDI_Player
// v2 - updated note symbols to match octave numbers in homework.
// WKE - 9/22/25
//
package com.gt.music.midi;

import javax.sound.midi.*;
import java.util.HashMap;
import java.util.Scanner;

public class MIDI_Player {

    private static Receiver receiver = null;
    private final ShortMessage[] onMessages;
    private final ShortMessage[] offMessages;
    private HashMap<String, Integer> noteIndexes;
    private static final boolean[] channelsInUse = { false, false, false, false, false, false, false, false, false, false,
        false, false, false, false, false, false };
    private int channel = -1;
    private Instrument[] instruments;
    int instrumentNum = 0;
    static boolean debug = false;

    public static void setDebug(boolean d) {
	debug = d;
    }

    public MIDI_Player() {
        if (receiver == null) {
            try {
                receiver = MidiSystem.getReceiver();
            } catch (MidiUnavailableException e) {
                e.printStackTrace();
            }
        }

        int j = 0;
        while (j < 16) {
            if (!channelsInUse[j]) {
                channel = j;
                channelsInUse[j] = true;
                break;
            }
            j++;
        }
        if (channel == -1) {
            channel = 0;
        }

	// 4 octaves (= 48 notes), starting on note 48 (C3)
        onMessages = new ShortMessage[48];
        offMessages = new ShortMessage[48];
        try {
            for (int i = 0; i < 48; i++) {
                ShortMessage onMsg = new ShortMessage();
                onMsg.setMessage(ShortMessage.NOTE_ON, channel, 51 + i, 120);
                onMessages[i] = onMsg;

                ShortMessage offMsg = new ShortMessage();
                offMsg.setMessage(ShortMessage.NOTE_OFF, channel, 51 + i, 120);
                offMessages[i] = offMsg;
            }
            ShortMessage instrumentMsg = new ShortMessage();
            instrumentMsg.setMessage(ShortMessage.PROGRAM_CHANGE, channel, 0, 0);
            receiver.send(instrumentMsg, -1);
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }

        noteIndexes = new HashMap<>();
	int index = 48; // C3
        String[] notes = { "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B" };
	for (int h = 3 ; h < 7 ; h++) {  // Octave 3 through Octave 7
            for (int i = 0; i < 12; i++) {
                noteIndexes.put(notes[i] + h, index);
		if (debug) System.out.print("Added note: " + notes[i] + h + ": " + index);
		if (notes[i].endsWith("#")) {
		    String enharmonic = notes[(i+1) % notes.length].charAt(0) + "b";
		    noteIndexes.put(enharmonic + h, index);
		    if (debug) System.out.print(" (and enharmonic equivalent: " + enharmonic + h + ")");
		}
		if (debug) System.out.println();
                index++;
            }
        }
    }

    // Convert note to a canonical form. Basically this is toUpperCase but we can't use that since
    // the symbol for a flat is "b".
    public String canonicalize(String s) {
        if (s.length() == 2) {
	    return(s.toUpperCase());
        } if (s.length() == 3 && s.charAt(1) == '#') {
	    return s.toUpperCase();
	} else if (s.length() == 3 && s.charAt(1) == 'b') {
	    return new String(Character.toUpperCase(s.charAt(0)) + "b" + s.charAt(2));
	} else {
	    return s.toUpperCase();
	}
    }

    public void playMidiSound(int noteIndex) {
        receiver.send(onMessages[noteIndex-48], -1);
    }
    public void playMidiSound(String noteName) {
	noteName = canonicalize(noteName);
	if (debug) System.out.println("Canonicalized note to " + noteName);
        if (noteIndexes.keySet().contains(noteName)) {
 	    int val = noteIndexes.get(noteName);
	    if (debug) System.out.println("Playing note index " + val);
            playMidiSound(noteIndexes.get(noteName));
	}
    }

    public void stopMidiSound(int noteIndex) {
        receiver.send(offMessages[noteIndex-48], -1);
    }
    public void stopMidiSound(String noteName) {
	noteName = canonicalize(noteName);
        if (noteIndexes.keySet().contains(noteName)) {
            stopMidiSound(noteIndexes.get(noteName));
        }
    }
    public Instrument[] getInstruments() {
        if (instruments == null) {
            try {
                Synthesizer synth = MidiSystem.getSynthesizer();
                Soundbank bank = synth.getDefaultSoundbank();
                MidiSystem.getSynthesizer().loadAllInstruments(bank);
                instruments = synth.getAvailableInstruments();
                return instruments;
            } catch (MidiUnavailableException e) {
                return null;
            }
        } else {
            return instruments;
        }
    }
    public void setInstrument(int instrumentNumber) {
        instrumentNum = instrumentNumber;
        ShortMessage instrumentMsg = new ShortMessage();
        try {
            if (instrumentNumber >= 0 && instrumentNumber < 128) {
                instrumentMsg.setMessage(ShortMessage.PROGRAM_CHANGE, channel, instrumentNumber, 0);
            }
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }
        receiver.send(instrumentMsg, -1);
    }
    public String getInstrumentName() {
        return getInstruments()[instrumentNum].getName();
    }

    public void close() {
        //receiver.close();
        channelsInUse[channel] = false;
    }
	
	public static void main(String[] args) {
		MIDI_Player player = new MIDI_Player();
		Scanner scanner = new Scanner(System.in);
		
		System.out.println("Current instrument is " + player.getInstrumentName());
		System.out.println("Enter valid note symbols (e.g., either C4 or G#5), one per line");
		System.out.println("The range of notes is C3 - B6.");
		System.out.println("Hit Ctrl-D to end.");
		while (scanner.hasNextLine()) {
			String s = scanner.nextLine();
			System.out.println("You entered " + s);
			player.playMidiSound(s);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException ex) {
				System.out.println("Interrupted.");
			}
			player.stopMidiSound(s);
		}
	}
}
