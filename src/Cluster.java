import java.io.FileInputStream;
import java.io.IOException;
import java.lang.Math;
import java.util.*;

public class Cluster {
    private static Integer number_of_bills;
    private String training_dir;
    private LinkedHashMap<Integer, HashMap<Integer, ArrayList<String>>> clusters = new LinkedHashMap<>(); //Congress votes in each cluster; <cluster number, <Congress Member: [yes, nay, nay, ...]>>

    private Cluster(int number_bills, String train_dir) throws IOException {
        number_of_bills = number_bills;
        training_dir = train_dir;
        init();
    }

    /*
    Initialization will read the training data and initialize each cluster for Agglomerative algorithm
    */
    private void init() throws IOException {
        //initialize containers for the clusters and the cluster key
        for(int x = 0; x < number_of_bills; x++) {
            clusters.put(x, new HashMap<>());
        }

        //read the file and fill in the cluster data structures
        int cluster_number = 0;

        try (FileInputStream inputStream = new FileInputStream(training_dir); Scanner sc = new Scanner(inputStream, "UTF-8")) {
            while (sc.hasNextLine()) {
                //read and reformat each line in the file. Each line represents the votes for each congressmen
                String[] line = sc.nextLine().split(",");
                ArrayList<String> filtered_line = new ArrayList<>(Arrays.asList(line).subList(0, number_of_bills));
                HashMap<Integer, ArrayList<String>> member_vote = new HashMap<>();
                member_vote.put(cluster_number, filtered_line);

                //add reformatted list to the hashmap
                clusters.put(cluster_number, member_vote);

                //update current cluster index
                cluster_number++;
            }
            if (sc.ioException() != null) {
                throw sc.ioException();
            }
        }

        //debugging
        //for(HashMap.Entry entry: clusters.entrySet()) {
        //    System.out.println(entry);
        //}
    }

    private void AgglomerativeAlgorithm(int cluster_size) {
        while(clusters.size() > cluster_size) {
            //initialize variables used to track smallest jacard distance between all clusters
            int cluster_A_index = -1;
            int cluster_B_index = -1;
            double smallest_jacard_distance = Double.MAX_VALUE;

            //cluster A and B
            HashMap<Integer, ArrayList<String>> cluster_A;
            HashMap<Integer, ArrayList<String>> cluster_B;
            double current_jaccard_distance;

            //find the smallest jaccard distance by calculating distance for all cluster combinations
            for(int x: clusters.keySet()) { //int x = 0; x < clusters.size(); x++
                for (Integer y: clusters.keySet()){ //int y = 0; y < clusters.size(); y++
                    //we wont compare the distance from a cluster to itself
                    if(x == y) {
                        continue;
                    }
                    //if one of the clusters has been removed continue to new cluster pair
                    if(clusters.get(x) == null || clusters.get(y) == null) {
                        continue;
                    }
                    //get clusters jacard distance
                    cluster_A = clusters.get(x);
                    cluster_B = clusters.get(y);

                    //get shortest jacard distance
                    current_jaccard_distance = averageLinkValue(cluster_A, cluster_B);

                    //if the current cluster pair has the smallest jacard distance record this information
                    if(current_jaccard_distance < smallest_jacard_distance) {
                        cluster_A_index = x;
                        cluster_B_index = y;
                        smallest_jacard_distance = current_jaccard_distance;
                    }
                }
            }
            mergeClusters(cluster_A_index, cluster_B_index);
        }

    }

    //gets the averagelink value with jaccard index
    private double averageLinkValue(HashMap<Integer, ArrayList<String>> A, HashMap<Integer, ArrayList<String>> B) {
        double averageLink = 0.00; //sum of all jaccard distances

        //gets the rolling sum of all jaccard distances between both clusters
        for(ArrayList<String> voterA: A.values()) {
            for(ArrayList<String> voterB: B.values()) {
                averageLink += (JaccardDistance(voterA, voterB) * 100);
            }
        }

        //average linking is the average distance between connecting points in cluster a and b
        //L(A,B) = 1/(n_A*n_B) * (sum of all distances between clusters)
        averageLink *= 1.0/(A.size() * B.size());

        return averageLink;
    }

    /*
     Jaccard Index = J(X,Y) = |X∩Y| / |X∪Y| = (the number in both sets)/(the number in either set)*100

     1) Count the number of members which are shared between both sets.
     2) Count the total number of members in both sets (shared and un-shared).
     3) Divide the number of shared members (1) by the total number of members (2).
     4) Multiply the number you found in (3) by 100.

     Example:
         A = {0,1,2,5,6}
         B = {0,2,3,4,5,7,9}
         Solution: J(A,B) = |A∩B| / |A∪B| = |{0,2,5}| / |{0,1,2,3,4,5,6,7,9}| = 3/9 = 0.33.
  */
    private double JaccardDistance(ArrayList<String> A, ArrayList<String> B) {
        //deceleration of variables we will be using
        double distance;
        double A_union_B_value = 0.00;
        double A_intersect_B_Value = number_of_bills; //Believe this value is always equal to the number of bills

        // counts the number of members that are shared between both sets.
        for(int x = 0; x < number_of_bills; x++) {
            if(A.get(x).equals(B.get(x))) {
                A_union_B_value ++;
            }
        }
        //final Jaccard index calculation
        distance = 1 - (Math.abs(A_union_B_value)/Math.abs(A_intersect_B_Value));

        return distance;
    }

    private void mergeClusters(int clusterA, int clusterB) {
        if(clusterA == -1 || clusterB == -1) {
            System.out.println("Error, no smallest cluster found for some reason.");
            System.exit(0);
        }

        //now try merging them
        HashMap<Integer, ArrayList<String>> copy_cluster_b = clusters.get(clusterB); //make a copy in case

        for(Map.Entry<Integer, ArrayList<String>> entry: copy_cluster_b.entrySet()) { //add all values in cluster B to cluster A
            clusters.get(clusterA).put(entry.getKey(), entry.getValue());
        }

        //delete cluster B from clusters after merge
        clusters.remove(clusterB);

       // System.out.println("# Clusters: " + clusters.size());

    }

    public static void main(String args[]) throws IOException {
        //TODO: uncomment for final submission
        //Cluster cluster = new Cluster(42, args[1]);
        //cluster.AgglomerativeAlgorithm(Integer.parseInt(args[2]));

        Cluster cluster = new Cluster(42, "data/congress_train.csv");
        cluster.AgglomerativeAlgorithm(10);

        ArrayList<ArrayList<Integer>> result = new ArrayList<>();

        //sort the clusters by values within them and then add to a new list that will hold all the clusters
        for(HashMap.Entry<Integer, HashMap<Integer, ArrayList<String>>> entry: cluster.clusters.entrySet()) {

            HashMap<Integer, ArrayList<String>> congress_in_cluster = entry.getValue();

            ArrayList<Integer> sort = new ArrayList<>();

            for(Map.Entry<Integer, ArrayList<String>> congress_member: congress_in_cluster.entrySet()) {
                sort.add(congress_member.getKey());
            }
            Collections.sort(sort);
            result.add(sort);
        }

        //now sort the final list of clusters and print in the proper format
        result.sort(Comparator.comparing(l -> l.get(0)));

        for(ArrayList<Integer> cluster_result: result) {
            //TODO: print it as plain text and not a list
            System.out.println(cluster_result.toString().replace("[", "").replace("]", ""));
        }
    }
}
