package structures;

import structures.basic.Card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a player's hand (max 6 cards); supports add/remove.
 */
public class Hand {
    public static final int MAX = 6;
    private final List<Card> cards = new ArrayList<>();

    public List<Card> getCardsView() {
        return Collections.unmodifiableList(cards);
    }

    public int size() { return cards.size(); }

    public Card get(int index) {
        if (index < 0 || index >= cards.size()) return null;
        return cards.get(index);
    }

    public boolean contains(Card c) {
        return cards.contains(c);
    }

    public boolean add(Card c) {
        if (cards.size() >= MAX) return false;
        cards.add(c);
        return true;
    }

    public Card removeAt(int index) {
        if (index < 0 || index >= cards.size()) return null;
        return cards.remove(index);
    }
}
