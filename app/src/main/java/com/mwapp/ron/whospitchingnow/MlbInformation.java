package com.mwapp.ron.whospitchingnow;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * All information associated with the mlb.com lookup
 */
public class MlbInformation {
    public static class InvalidStateException extends RuntimeException {}

    private String teamID;
    private String pitcherName;
    private String era;
    private String wins, losses;
    private String pitcherNumber;
    private String opposingTeam;

    private boolean weAreAway;

    public MlbInformation(String teamID) throws IOException, ParserConfigurationException, SAXException {
        this.teamID = teamID;
        Date today = new Date();
        SimpleDateFormat year = new SimpleDateFormat("yyyy");
        SimpleDateFormat month = new SimpleDateFormat("MM");
        SimpleDateFormat day = new SimpleDateFormat("dd");
        String baseURL = "http://gd2.mlb.com/components/game/mlb/year_" + year.format(today)
                + "/month_" + month.format(today) + "/day_" + day.format(today) + "/";

        URL url = new URL(baseURL + "/master_scoreboard.xml");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
        {
            Log.e("WPN", "Unable to connect to MLB " + connection.getResponseCode());
            throw new IOException("Unable to connect to MLB " + connection.getResponseCode());
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document scoreBoard = builder.parse(connection.getInputStream());
        NodeList games = scoreBoard.getElementsByTagName("game");
        for (int i = 0; i < games.getLength(); i++)
        {
            Node game = games.item(i);
            NamedNodeMap attributes = game.getAttributes();
            Node awayCode = attributes.getNamedItem("away_code");
            if (awayCode.getNodeValue().equals(teamID))
            {
                weAreAway = true;
                handleDataFromGame(game, attributes);
                return;
            }
            Node homeCode = attributes.getNamedItem("home_code");
            if (homeCode.getNodeValue().equals(teamID))
            {
                weAreAway = false;
                handleDataFromGame(game, attributes);
                return;
            }

        }

    }

    private void handleDataFromGame(Node game, NamedNodeMap gameAttributes)
    {
        this.opposingTeam = weAreAway ?
                gameAttributes.getNamedItem("home_team_city").getNodeValue() + " " + gameAttributes.getNamedItem("home_team_name").getNodeValue() :
                gameAttributes.getNamedItem("away_team_city").getNodeValue() + " " + gameAttributes.getNamedItem("away_team_name");

        Node statusNode = game.getFirstChild().getNextSibling();
        NamedNodeMap status = statusNode.getAttributes();

        String statusString = status.getNamedItem("status").getNodeValue();
        if (statusString.equals("Final")) {
            this.pitcherName = "Game Ended";
            this.era = "Game Ended";
            return;
        }
        String expectedTag;
        if (statusString.equals("In Progress"))
        {
            boolean topInning = status.getNamedItem("top_inning").getNodeValue().equals("Y");
            expectedTag = (weAreAway ^ topInning) ? "pitcher" : "opposing_pitcher";

        }
        else if (statusString.equals("Preview") || statusString.equals("Pre-Game"))
        {
            expectedTag = weAreAway ? "away_probable_pitcher" : "home_probable_pitcher";
        }
        else
        {
            throw new InvalidStateException();
        }
        NodeList childTags = game.getChildNodes();
        for (int i = 0; i < childTags.getLength(); i++)
        {
            if (childTags.item(i).getNodeName().equals(expectedTag)) //This is the pitcher tag.
            {
                NamedNodeMap attributes = childTags.item(i).getAttributes();
                pitcherName = attributes.getNamedItem("first").getNodeValue() + " " + attributes.getNamedItem("last").getNodeValue();
                era = attributes.getNamedItem("era").getNodeValue();
                wins = attributes.getNamedItem("wins").getNodeValue();
                losses = attributes.getNamedItem("losses").getNodeValue();
                pitcherNumber = attributes.getNamedItem("number").getNodeValue();
            }
        }
    }

    public String getPitcherName()
    {
        return pitcherName;
    }

    public  String getERA()
    {
        return era == null ? "Error" : era;
    }

    public String getOpposingTeam() {
        return opposingTeam;
    }

    public String getOtherStatlist()
    {
        return  "Number: " + pitcherNumber + "\nWins: " + wins + "\nLosses: " + losses;
    }
}
