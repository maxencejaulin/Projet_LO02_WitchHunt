package com.model.card.effect;

import com.controller.RoundController;
import com.model.card.CardName;
import com.model.card.RumourCard;
import com.model.player.Player;

public class TakeFromAccuserHandEffect extends Effect {
    @Override
    public String toString() {
        return """
                Take one card from the hand of
                the player who accused you.""";
    }

    @Override
    public boolean applyEffect(final Player cardUser, final Player target) {
        
    	if(target.getSelectableCardsFromHand().size() >= 1) {
    		RumourCard chosenCard = RoundController.getRoundController().chooseCard(cardUser, target.getSelectableCardsFromHand());
            cardUser.addCardToHand(target.removeCardFromHand(chosenCard));
            return true;
    	} else {
    		return false;
    	} 
    }

    @Override
    public Player chooseTarget(final CardName cardName, Player cardUser) {
        return RoundController.getCurrentPlayer();
    }

}
