package structures;

import structures.basic.Card;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Represents a player's draw pile; supports draw logic.
 */
public class Deck {
    private final Deque<Card> cards = new ArrayDeque<>();

    public void pushToTop(Card c) { if (c != null) cards.push(c); }
    public void addToBottom(Card c) { if (c != null) cards.addLast(c); }

    public Card drawTop() {
        return cards.pollFirst();
    }

    public int size() { return cards.size(); }
}
