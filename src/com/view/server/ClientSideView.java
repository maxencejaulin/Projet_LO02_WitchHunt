package com.view.server;

import com.controller.PlayerAction;
import com.controller.RoundController;
import com.model.card.CardName;
import com.model.card.RumourCard;
import com.model.game.LocalRound;
import com.model.game.Round;
import com.view.ActiveView;
import com.view.PassiveView;
import com.view.View;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

public class ClientSideView implements ActiveView, PassiveView {
	private static final int ADDRESS_MIN = 0;
	private static final int ADDRESS_MAX = 256;
	private static final int PORT_MIN = 49152;
	private static final int PORT_MAX = 49160;

	private Terminal server;

	private final View view;

	public ClientSideView(PassiveView view) {
		this.view = view;
		new RoundController((ActiveView) view);
		connectToAvailableHost();
	}

	///////////////////////////////////////////////////////////////////////////
	// Connection method
	///////////////////////////////////////////////////////////////////////////

	public void connectToAvailableHost() {
		try {
			byte[] ip = InetAddress.getLocalHost().getAddress();
			ExecutorService executor = ForkJoinPool.commonPool();
			server = executor.invokeAny(
					IntStream.range(ADDRESS_MIN, ADDRESS_MAX).unordered().parallel()
							.mapToObj(i -> new byte[]{ip[0], ip[1], (byte) i, 0})
							.flatMap(bytes -> IntStream.range(ADDRESS_MIN, ADDRESS_MAX).mapToObj(j -> new byte[]{bytes[0], bytes[1], bytes[2], (byte) j}))
							.<CallableTerminal>mapMulti((bytes, consumer) -> {
								try {
									InetAddress inetAddress = InetAddress.getByAddress(bytes);
									IntStream.range(PORT_MIN, PORT_MAX).forEach(i -> consumer.accept(new CallableTerminal(inetAddress, i)));
								} catch (UnknownHostException e) {
									e.printStackTrace();
								}
							})
							.toList()
			);

			System.out.println("Found host: " + server.socket());
			executor.shutdown();
		} catch (InterruptedException | ExecutionException | UnknownHostException e) {
			e.printStackTrace();
		}
	}

	///////////////////////////////////////////////////////////////////////////
	// Communication methods
	///////////////////////////////////////////////////////////////////////////

	@SuppressWarnings("unchecked")
	private void chooseAppropriatePassiveAction(ExchangeContainer c) {
		switch (c.state) {
			case SHOW_CARD_LIST -> showCardList(c.name, (List<String>) c.list);
			case SHOW_GAME_WINNER -> showGameWinner(c.name, c.nb);
			case SHOW_ROUND_WINNER -> showRoundWinner(c.name);
			case SHOW_ACCUSE_ACTION -> showPlayerAction(c.name, c.name2);
			case SHOW_REVEAL_ACTION -> showPlayerAction(c.name);
			case SHOW_START_OF_ROUND -> showStartOfRound(c.nb);
			case SHOW_CARD_USE_ACTION -> showPlayerAction(c.name, c.cardName);
			case SHOW_PLAYER_IDENTITY -> showPlayerIdentity(c.name, c.isWitch);

			case ACTION_WAIT -> waitForAction(c.name, (List<PlayerAction>) c.list);
			case NEW_GAME_WAIT -> waitForNewGame();
			case CARD_CHOICE_WAIT -> waitForCardChoice((List<RumourCard>) c.list);
			case REPARTITION_WAIT -> waitForRepartition();
			case PLAYER_NAME_WAIT -> waitForPlayerName(c.nb);
			case PLAYER_CHOICE_WAIT -> waitForPlayerChoice((List<String>) c.list);
			case PLAYER_SWITCH_WAIT -> waitForPlayerSwitch(c.name);
			case PLAYER_IDENTITY_WAIT -> waitForPlayerIdentity(c.name);
			case BLANK_CARD_CHOICE_WAIT -> waitForCardChoice(null);

			default -> System.err.println("Wrong passive action : " + c.state);
		}
	}

	@SuppressWarnings("unchecked")
	private Object chooseAppropriateActiveAction(ExchangeContainer c) {
		return switch (c.state) {
			case ACTION_REQUEST -> promptForAction(c.name, (List<PlayerAction>) c.list);
			case NEW_GAME_REQUEST -> promptForNewGame();
			case CARD_CHOICE_REQUEST -> promptForCardChoice(c.name, (List<RumourCard>) c.list);
			case REPARTITION_REQUEST -> promptForRepartition();
			case PLAYER_NAME_REQUEST -> promptForPlayerName(c.nb);
			case PLAYER_CHOICE_REQUEST -> promptForPlayerChoice(c.name, (List<String>) c.list);
			case PLAYER_SWITCH_REQUEST -> {
				promptForPlayerSwitch(c.name);
				yield null;
			}
			case PLAYER_IDENTITY_REQUEST -> promptForPlayerIdentity(c.name);
			case BLANK_CARD_CHOICE_REQUEST -> promptForCardChoice(c.name, c.nb);
			default -> throw new IllegalStateException("Unexpected value: " + c.state);
		};
	}

	public void run() {
		try {
			while (server != null) {
				try {
					Object object = server.input().readObject();

					if (object instanceof Round round) {
						LocalRound.setInstance(round);
					} else if (Round.getInstance() != null && object instanceof ExchangeContainer container) {
						if (container.state.isActive()) {
							server.output().writeObject(chooseAppropriateActiveAction(container));
						} else {
							chooseAppropriatePassiveAction(container);
						}
					} else {
						System.err.println("Received non standard transmission : " + object);
					}
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		} catch (SocketException e) {
			System.err.println("The server was closed, shutting down...");
			System.exit(0);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	///////////////////////////////////////////////////////////////////////////
	// View methods
	///////////////////////////////////////////////////////////////////////////

	@Override
	public void showGameWinner(String name, int numberOfRound) {
		view.showGameWinner(name, numberOfRound);
	}

	@Override
	public void showRoundWinner(String name) {
		view.showRoundWinner(name);
	}

	@Override
	public void showStartOfRound(int numberOfRound) {
		view.showStartOfRound(numberOfRound);
	}

	@Override
	public void showPlayerIdentity(String name, boolean witch) {
		view.showPlayerIdentity(name, witch);
	}

	@Override
	public void showPlayerAction(String name) {
		view.showPlayerAction(name);
	}

	@Override
	public void showPlayerAction(String name, String targetedPlayerName) {
		view.showPlayerAction(name, targetedPlayerName);
	}

	@Override
	public void showPlayerAction(String name, CardName chosenCardName) {
		view.showPlayerAction(name, chosenCardName);
	}

	@Override
	public void showCardList(String name, List<String> cards) {
		view.showCardList(name, cards);
	}

	///////////////////////////////////////////////////////////////////////////
	// Active methods
	///////////////////////////////////////////////////////////////////////////

	@Override
	public String promptForPlayerName(int playerIndex) {
		return ((ActiveView) view).promptForPlayerName(playerIndex);
	}

	@Override
	public String promptForNewGame() {
		return ((ActiveView) view).promptForNewGame();
	}

	@Override
	public int promptForPlayerChoice(String playerName, List<String> playerNames) {
		return ((ActiveView) view).promptForPlayerChoice(playerName, playerNames);
	}

	@Override
	public int promptForCardChoice(String playerName, List<RumourCard> rumourCards) {
		return ((ActiveView) view).promptForCardChoice(playerName, rumourCards);
	}

	@Override
	public int promptForCardChoice(String playerName, int listSize) {
		return ((ActiveView) view).promptForCardChoice(playerName, listSize);
	}

	@Override
	public int[] promptForRepartition() {
		return ((ActiveView) view).promptForRepartition();
	}

	@Override
	public int promptForPlayerIdentity(String name) {
		return ((ActiveView) view).promptForPlayerIdentity(name);
	}

	@Override
	public PlayerAction promptForAction(String playerName, List<PlayerAction> possibleActions) {
		return ((ActiveView) view).promptForAction(playerName, possibleActions);
	}

	@Override
	public void promptForPlayerSwitch(String name) {
		((ActiveView) view).promptForPlayerSwitch(name);
	}

	///////////////////////////////////////////////////////////////////////////
	// Passive Methods
	///////////////////////////////////////////////////////////////////////////

	@Override
	public void waitForPlayerName(int playerIndex) {
		((PassiveView) view).waitForPlayerName(playerIndex);
	}

	@Override
	public void waitForNewGame() {
		((PassiveView) view).waitForNewGame();
	}

	@Override
	public void waitForPlayerChoice(List<String> playerNames) {
		((PassiveView) view).waitForPlayerChoice(playerNames);
	}

	@Override
	public void waitForCardChoice(List<RumourCard> rumourCards) {
		((PassiveView) view).waitForCardChoice(rumourCards);
	}

	@Override
	public void waitForRepartition() {
		((PassiveView) view).waitForRepartition();
	}

	@Override
	public void waitForPlayerIdentity(String name) {
		((PassiveView) view).waitForPlayerIdentity(name);
	}

	@Override
	public void waitForAction(String playerName, List<PlayerAction> possibleActions) {
		((PassiveView) view).waitForAction(playerName, possibleActions);
	}

	@Override
	public void waitForPlayerSwitch(String name) {
		((PassiveView) view).waitForPlayerSwitch(name);
	}
}