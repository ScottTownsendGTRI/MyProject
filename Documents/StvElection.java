/**
 * StvElection.java
 * Purpose: Count and simulate the process of electing candidates through an STV-style Election.
 * Entry file format: line 1: number of participants, line 2: line number of available positions, line 3-n: participant names, line n+1: 0 for Hare or anything else for Droop.
 * Vote file format: single numbers on each line, follows the order for the voter's choice (1 for most wanted, n for least wanted). In the case of not ordering certain candidates, -1 is the placeholder.
 * VotingSlip and Participant classes maintained within this source code.
 * CAUTION: In the case of eliminating the lowest voted candidate and multiple candidates are tied at the bottom, the one closer to the beginning of the list will be eliminated.
 * Note: Elect algorithm lowers the weight of votes and moves all of them to the next candidate instead of keeping the votes who elected someone with them. 
 * 
 * @version 1.1 5/11/15
 * @author Scott Townsend
 */

import java.util.ArrayList;
import java.util.Scanner;
import java.io.*;

public class StvElection {  
    private int numOfParticipants = 0;
    private int numOfPositions;
    private int numOfVotes;
    private boolean hareVoting;
    private ArrayList<Participant> participants;
    private double votesGiven[];
    
    /**
     * Empty constructor
     */
    public StvElection() {}
    
    /**
     * Constructor
     * 
     * @param file File that holds the necessary participant data
     */
    public StvElection(File file) throws FileNotFoundException {
        newElection(file);
    }
    
    /**
     * Creates a new election based around the given file
     * 
     * @param file File that holds the necessary participant data
     */
    public void newElection(File file) throws FileNotFoundException {
        Scanner input = new Scanner(file);
        numOfParticipants = Integer.parseInt(input.nextLine());
        numOfPositions = Integer.parseInt(input.nextLine());
        
        // Adds the participants to the list
        participants = new ArrayList<Participant>(numOfParticipants);
        for (int i = 0; i < numOfParticipants; i++) {
            participants.add(new Participant(input.nextLine()));
        }
        // Decides what type of STV Election is desired (Hare or Droop)
        if (Integer.parseInt(input.nextLine()) == 0) {
            hareVoting = true;
        }
        else {
            hareVoting = false;
        }
            
        votesGiven = new double[numOfParticipants];
        numOfVotes = 0;
    }
    
    /**
     * Adds an individual vote to the election data
     * 
     * @param voteFile File that holds a specific voter's choices
     */
    public void addVote(File voteFile) throws FileNotFoundException {
        if (numOfParticipants < 1) {
            System.out.println("No Participant file given.");
            System.exit(0);
        }
        
        Scanner input = new Scanner(voteFile);
        VotingSlip slip = new VotingSlip(numOfParticipants);
        
        // Moves the vote to the voter's top choice
        int topVote = -1, vote;
        for (int i = 0; i < numOfParticipants; i++) {
            vote = Integer.parseInt(input.nextLine());
            slip.getSequence()[i] = vote;
            if (vote == 1) {
                topVote = i;
            }
        }
        participants.get(topVote).add(slip);
        
        numOfVotes++;
    }
    
    /**
     * Utilizes the given participant and votes data, and elects the candidates
     * 
     * @return String
     */
    public String elect() {
        addUpTotalVotes();
        checkThreshold();
        
        // Adds the winners' names to a String separated by commas
        String winners = "";
        for (Participant p : participants) {
            if (p.wasElected()) {
                winners = winners + p.getName() + ", ";
            }
        }
        return winners.substring(0, winners.length()-2);
    }
    
    /**
     * Adds up the current total votes of each participant during the election. The total votes change throughout the election as votes are moved once candidates are elected/eliminated.
     */
    private void addUpTotalVotes() {
        // Locates each participants current votes and add up the weights
        for (int i = 0; i < numOfParticipants; i++) {
            double v = 0.0;
            for (VotingSlip vote : participants.get(i).getSlips()) {
                v = v + vote.getWeight();
            }
            votesGiven[i] = v;
        }

    }
   
    /**
     * The start of the recursive algorithm.
     * Elects the rest of the candidates if the remaining number candidates and positions match. Otherwise, it checks if a candidate receives the necessary number of votes and elects them if they do.
     * If no candidate meets the required number of votes, it eliminates the participant with the lowest number of votes.
     */
    
    private void checkThreshold() {
        while(true) {
        // Elects the remaining candidates if they match the number of positions
        if (exactCandidatesRemaining()) {
            for (Participant p : participants) {
                if (!p.wasElected() && !p.isDead()) {
                    p.elect();
                }
            }
            return; /** This is where the recursive algorithm ends*/
        }

        // Checks if a candidate has enough votes
        int neededVotes;
        if (hareVoting) {
            neededVotes = numOfVotes / numOfPositions;
        }
        else {
            neededVotes = (numOfVotes / (numOfPositions+1)) + 1;
        }
        
        boolean someoneElected = false;
        for (int i = 0; i < numOfParticipants; i++) {
            if (votesGiven[i] >= neededVotes) {
                participants.get(i).elect();
                someoneElected = true;
            }
        }
        
        //Eliminates a candidate if none were elected
        if (!someoneElected)
            eliminate();
        
        moveVotes();
        }
    }
    
    /**
     * Checks if there is only the perfect number of candidates left for the remaining positions
     * 
     * @return boolean
     */
    
    private boolean exactCandidatesRemaining() {
        int participantsLeft = 0, participantsElected = 0;
        for (Participant p : participants) {
            if (!p.isDead()) {
                participantsLeft++;
            }
            if (p.wasElected()) {
                participantsElected++;
            }                
        }
        if (participantsLeft == numOfPositions - participantsElected) {
            return true;
        }
        return false;
    }
    
    /**
     * Eliminates the participant with the lowest number of votes.
     */
    
    private void eliminate() {
        int lowestParticipant = -1;
        
        for (int i = 0; i < numOfParticipants; i++) {
            if (!participants.get(i).isDead()) {
                lowestParticipant = i;
                break;
            }
        }
        
        for (int i = 0; i < numOfParticipants; i++) {
            if (votesGiven[i] < votesGiven[lowestParticipant] && !participants.get(i).isDead()) {
                lowestParticipant = i;
            }
        }
        
        participants.get(lowestParticipant).kill();
    }
    
    /**
     * Moves the votes from candidates who are elected/eliminated to their next candidate choice. Lowers the weight of the vote if necessary.
     */
    private void moveVotes() {
        for (Participant p : participants) {
            if (p.wasElected() || p.isDead()) {
                // Cycles through their votes because they need to be transferred
                for (VotingSlip vote : p.getSlips()) {
                    // Moves the votes and lowers their weight if elected.
                    if (p.wasElected()) {
                        int neededVotes;
                        if (hareVoting) {
                            neededVotes = numOfVotes / numOfPositions;
                        }
                        else {
                            neededVotes = (numOfVotes / (numOfPositions+1)) + 1;
                        }                        
                        vote.lowerWeight((p.getSlips().size() - neededVotes)/p.getSlips().size());
                    }

                    // Moves the votes to the next choice unless no choices remain on the voter's list. If so, the vote is eliminated. -1 is the placeholder for no choice.
                    int nextCandidate;
                    while (true) {
                        nextCandidate = vote.getNextChoice();
                        if (nextCandidate == -1) {
                            vote.kill();
                            break;
                        }
                        if (!participants.get(nextCandidate).isDead() && !participants.get(nextCandidate).wasElected()) {
                            break;
                        }
                    }
                    if (!vote.isDead()) {
                        participants.get(nextCandidate).add(vote);
                    }
                }
                p.removeVotes();
            }
        }
        addUpTotalVotes();
    }
    
    /*
     * Deprecated method
     */
    
    /*
    private boolean allPositionsFilled() {
        int positionsFilled = 0;
        for (Participant p : participants) {
            if (p.wasElected()) {
                positionsFilled++;
            }
        }
        if (positionsFilled == numOfPositions) {
            return true;
        }
        return false;
    }
    */
}

/**
 * VotingSlip.java
 * Purpose: Hold the data for a specific voter's choices.
 * 
 * @version 1.1 4/12/15
 * @author Scott Townsend
 */

class VotingSlip {
    private int voteSequence[];
    private int currentChoice = 1;
    private double weight = 1.0;
    private boolean dead = false;
    
    /**
     * Constructor
     * 
     * @param participants The number of total participants in the election.
     */
    public VotingSlip(int participants) {
        voteSequence = new int[participants];
    }

     /**
     * Increments the voter's current choice and then locates and returns what position in the candidate list that participant is in.
     * 
     * @return int
     */
    public int getNextChoice() {
        currentChoice++;
        for (int i = 0; i < voteSequence.length; i++) {
            if (voteSequence[i] == currentChoice) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Lowers the current weight of the vote.
     * 
     * @param lowerValue The percentage of the current value that will be passed on to the voter's next choice.
     */
    public void lowerWeight(int lowerValue) {
        weight = weight * lowerValue;
    }
    
    /**
     * Returns the weight of the vote.
     * 
     * @return double
     */
    public double getWeight() {
        return weight;
    }
    
    /**
     * Kills the vote because it is no longer used.
     */
    public void kill() {
        dead = true;
    }
    
    /**
     * Returns if the vote is still active
     * 
     * @return boolean
     */
    public boolean isDead() {
        return dead;
    }
    
    /**
     * Returns the voter's voting sequence.
     * 
     * @return int[]
     */
    public int[] getSequence() {
        return voteSequence;
    }
}

/**
 * Participant.java
 * Purpose: Hold a single participant's number of votes and whether they were elected or eliminated
 * 
 * @version 1.1 4/12/15
 * @author Scott Townsend
 */
class Participant {
    private String name;
    private ArrayList<VotingSlip> votes = new ArrayList<VotingSlip>();
    private boolean elected = false, dead = false;
    
    /**
     * Constructor
     * 
     * @param String The name of the participant.
     */
    public Participant(String n) {
        name = n;
    }
    
    /**
     * Returns the name of the participant
     * 
     * @return String
     */
    public String getName() {
        return name;
    }
    
    /**
     * Adds a new voting slip to this participant's total.
     * 
     * @param VotingSlip
     */
    public void add(VotingSlip slip) {
        votes.add(slip);
    }
    
    /**
     * Eliminates the participant
     */
    public void kill() {
        dead = true;
    }
    
    /**
     * Returns whether the participant has been eliminated.
     * 
     * @return boolean
     */
    public boolean isDead() {
        return dead;
    }
    
    /**
     * Elects the candidate
     */
    public void elect() {
        elected = true;
    }
    
    /**
     * Returns whether the participant has been elected.
     * 
     * @return boolean
     */
    public boolean wasElected() {
        return elected;
    }
    
    /**
     * Removes the votes.
     */
    public void removeVotes() {
        votes = new ArrayList<VotingSlip>(0);
    }
    
    /**
     * Returns the list of voting slips
     */
    public ArrayList<VotingSlip> getSlips() {
        return votes;
    }
}