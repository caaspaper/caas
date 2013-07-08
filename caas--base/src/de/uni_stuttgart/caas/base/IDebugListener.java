package de.uni_stuttgart.caas.base;

/** Abstract listener for debugging messages.
 *
 *  Intended implementations include a log listener who writes debug
 *  messages to a (local) log file and a network listener who forwards
 *  them to i.e. an admin node.
 */
public interface IDebugListener 
{
	/** Called upon reception of a debug message.
	 *
	 *  The implementation need not be threadsafe.
	 *
	 *  @param message Message in plain-text, never null.  */
    public void write(String message);
}

