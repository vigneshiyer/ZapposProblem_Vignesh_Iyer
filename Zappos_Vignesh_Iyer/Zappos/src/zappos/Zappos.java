/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package zappos;
import java.io.IOException;
import java.net.*;
import java.io.*;
import java.util.Vector;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author vigneshiyer
 */

/*
    This class consists of all the methods to find the gift options for a given price and number of products.
*/
class ZapposProblemSolver
{
    private final String DEVELOPER_KEY = "&key=52ddafbe3ee659bad97fcce7c53592916a6bfd73";
    //sort value in the Zappos API call
    private final String SORT_PARAMS_ASC = "&sort={\"price\":\"asc\"}";
    //limit value in the Zappos API call
    private final String MAX_RESULTS = "&limit=100";
    private final int MAX_RESPONSE = 100;
    //count of how many calls have been made to the API. For efficiency purposes.
    private int totalAPICalls = 0;
    private int PAGE_NO = 1;
    //excludes value in the Zappos API call
    private final String EXCLUDE_PARAMS = "&excludes=[\"originalPrice\",\"styleId\",\"productUrl\",\"productId\",\"thumbnailImageUrl\",\"brandName\",\"colorId\",\"percentOff\"]";
    //base_url value in the Zappos API call
    private final String BASE_URL = "http://api.zappos.com/Search?term=";
    //facets value in the Zappos API call
    private final String PRICE_FACET = "&facets=[\"price\"]";
    //excludes results value in the Zappos API call
    private final String EXCLUDE_RESULTS = "&excludes=[\"results\"]";
    private String API_URL,statusCode;
    //flag is true if the totalResultCount is obtained by API
    private boolean totalResultsFlag = false;
    private URL url;
    private URLConnection urlConnection;
    private int totalResultCount;
    JSONObject root;
    JSONArray results,values,facets;
    BufferedReader in;
    
    // Stores the unique Zappos product prices as the key and the #of products as the value.
    TreeMap<Double,Integer> priceList = new TreeMap<Double,Integer>();
    
    // Stores the unique Zappos product prices as the key and the cumulative count of products as the value. 
    //this is used for direct calculation of page number in which the product of desired price is found.
    TreeMap<Double,Integer> cumulativepriceList = new TreeMap<Double,Integer>();
    
    //This stores the combinations of product price possible for a given amount and # of products.
    Vector<Double[]> priceCombinations;
    
    //This stores the unique produt prices of the priceCombinations calculated
    Vector<Double>productPrice = new Vector<>();
    
    //This stores the product price as key and all the product names available for this price as the key.
    HashMap<Double,String[]>products = new HashMap<>();
    
    /*
    This method initializes the required variables and objects.
    */
    public boolean initialize()
    {
        try {
            System.out.println("Getting product counts using price facet");
            API_URL = BASE_URL + DEVELOPER_KEY + PRICE_FACET + EXCLUDE_RESULTS;
            //System.out.println(API_URL);
            url = new URL(API_URL);
            urlConnection = url.openConnection();
            totalAPICalls++;
            in = new BufferedReader(new InputStreamReader(
                                urlConnection.getInputStream()));
            String input;
            StringBuilder strBuilder = new StringBuilder();
            while ((input = in.readLine()) != null)
                strBuilder.append(input);
            System.out.println("Response received. Parsing data");
            root = new JSONObject(strBuilder.toString());
            if(root!=null && (statusCode=root.getString("statusCode"))!=null && Integer.parseInt(statusCode) == 200 ) {
                facets = (JSONArray)root.get("facets");
                values = (JSONArray)((JSONObject)facets.get(0)).get("values");
                //System.out.println(values.length());
                
                //populating the priceList
                Integer sum=new Integer(0);
                for (int i = 0; i < values.length(); i++) {
                    priceList.put((Double)values.getJSONObject(i).getDouble("name"),(Integer)values.getJSONObject(i).getInt("count"));
                }
                
                // calculate cumulative count 
                System.out.println("Calculating cumulative counts for each product");
                Set set = priceList.entrySet();
                Iterator iterator = set.iterator();
                while(iterator.hasNext()) {

                  Map.Entry me2 = (Map.Entry)iterator.next();
                  sum+=(Integer)me2.getValue();
                  cumulativepriceList.put((Double)me2.getKey(), sum);
                }        
                

            }
            else
            {
                System.out.println("Zappos API returned no/invalid response for your query.");
                return false;
            }

        }
        catch(MalformedURLException ex) {
            System.out.println("Invalid URL");
            return false;
        }
        catch(IOException ex){
            System.out.println("URLConnection failed to open");
            return false;
        }
        catch(Exception ex) {
            System.out.println("Exception occured! "+ex.getMessage());
            return false;
        }
        return true;
    }
    
    /*
    This method calculates all the price combinations for the given amount 
    */
    public void getPriceCombinations(int n,double sum)
    {
        double[] arr;
        if(priceList.size()>sum)
            arr = new double[priceList.size()];
        else
            arr = new double[(int)sum];
        int pos=0;
        for (Map.Entry<Double, Integer> entrySet : priceList.entrySet()) {
            if(entrySet.getKey() <= sum)
                arr[pos++] = entrySet.getKey();                
            else
                break;            
        }
        double[] p =new double[pos+1];
        priceCombinations = new Vector<>();
        System.out.println("Calculating all possible price combinations for sum "+sum);
        compute(arr, p, 0, 0, pos, sum,n);
        
        
        System.out.println("Price combinations successfully calculated. (Total# "+priceCombinations.size());
        
        for (int i = 0; i < priceCombinations.size(); i++) {
            for (int j = 0; j < n; j++) {
                Double obj = priceCombinations.get(i)[j];
                if(!productPrice.contains(obj))
                    productPrice.add(obj);
            }           
        }       
        //System.out.println(productPrice.size());
        
    }
    
    
    
    /*
        This method recursively calls to get the price combinations
    */   
    public void compute(double[] arr,double[] p,int index,int low,int high,double sum,int n)
    {
        //System.out.println(index);
        if((sum<0)||(low>high)||index>n)
                 return;
        if(sum<=0.01 && index==n)
        {
            Double[] temp = new Double[n];
            for(int i=0;i<index;i++)
            {
                temp[i]=p[i];
                    //System.out.print(p[i]+" ");
            }
            //System.out.println();
            priceCombinations.add(temp);
            
            return;
        }
       p[index]=arr[low];
       compute(arr,p,index+1,low+1,high,sum-arr[low],n);
       compute(arr,p,index,low+1,high,sum,n);
    }
    
    /*
    This method gets the Zappos product details
    */
    public void getZapposProductDetails()
    {
        int size = productPrice.size();
        System.out.println("Calling Zappos API to fetch the products based on price with search term as blank.");
        for (int i = 0; i < size; i++) {
            //get the page number
            Double obj = productPrice.get(i);
            int page = cumulativepriceList.get(obj)/100+1;
            int elementNumber = cumulativepriceList.get(obj)%100-1;
            int count = priceList.get(obj);
            
            try{
                               
                
                String[] productNames = new String[count];
                int pos=0;
                while(count>0)
                {
                    //call api to get data
                    API_URL = BASE_URL + DEVELOPER_KEY + SORT_PARAMS_ASC + MAX_RESULTS+ "&page="+Integer.toString(page)+EXCLUDE_PARAMS;
                    url = new URL(API_URL);
                    urlConnection = url.openConnection();
                    System.out.println("Directly calling Zappos API (Page# "+page+") to get price for products with price $"+obj);
                    totalAPICalls++;
                    in = new BufferedReader(new InputStreamReader(
                                        urlConnection.getInputStream()));
                    String input;
                    StringBuilder strBuilder = new StringBuilder();
                    int j;
                    while ((input = in.readLine()) != null)
                        strBuilder.append(input);
                    root = new JSONObject(strBuilder.toString());
                    //System.out.println(root.getString("statusCode"));
                    if(root!=null && (statusCode=root.getString("statusCode"))!=null && Integer.parseInt(statusCode) == 200 ) {
                        for (j = elementNumber; j >= 0 && count>0; j--) {
                            //System.out.println(results.length());
                            JSONArray result =  ((JSONArray)root.get("results"));
                            JSONObject ele = (JSONObject)result.getJSONObject(j);
                            productNames[pos++]=ele.getString("productName");
                            //System.out.println(str);
                            count--;//sum+=Double.parseDouble(str.substring(1));                        
                        }
                        if(count>0)
                        {
                            page--;
                            elementNumber = MAX_RESPONSE-1;
                        }
                        else
                            elementNumber = j+1;
                        
                        
                    }
                    else
                        break;
                }
                products.put(obj, productNames);
                
                /*System.out.println(elementNumber);
                JSONArray result =  ((JSONArray)root.get("results"));
                            JSONObject ele = (JSONObject)result.getJSONObject(elementNumber);
                            String str = ele.getString("productName")+" "+ele.getString("price").replace(",", "");
                            System.out.println(str);*/
                
            }
            catch(MalformedURLException ex) {
                System.out.println("Invalid URL");
            }
            catch(IOException ex) {
                System.out.println("URLConnection failed to open");
            }
            catch(Exception ex) {
                System.out.println("Exception occured! "+ex.getMessage());
            }
            
            
            //break;
        }
        //calculate totalResultCount
        if(root.get("totalResultCount")!=null)
        {
            totalResultCount = Integer.parseInt((String)root.get("totalResultCount"));
            totalResultsFlag = true;
        }
        
    }
     
    /*
    Display the gift options
    */
    public void displayZapposProducts()
    {
        System.out.println("\n");
        System.out.println("Displaying gift options from Zappos for your input");
        int size = priceCombinations.size();
        System.out.println("There are in total "+size+" no of options available.");
        double sum=0;
        for (int i = 0; i < size; i++) {
            sum=0;
            System.out.println();
            System.out.println("**** OPTION "+(i+1)+" ****");
            Double[] arr = priceCombinations.get(i);
            for (int j = 0; j < arr.length; j++) {
                sum+=arr[j];
                System.out.println("For $"+arr[j]+" you can buy,");
                String[] productNames = products.get(arr[j]);
                for (int k = 0; k < productNames.length; k++) {
                    if(k==productNames.length-1)
                        System.out.println("("+productNames[k]+")");
                    else
                        System.out.println("("+productNames[k]+") OR ");
                }
                if(j!= arr.length-1)
                    System.out.println(" AND ");
            }
            System.out.println("Summary: "+arr.length+" order(s) for $"+sum);
            if((i+1)%10 == 0)
            {
                System.out.println("Displayed "+(i+1)+" options. There are about "+(size-i)+" more options. "
                        + "Press Enter key for more options. Press any key to exit");
                String input;
                Scanner sin = new Scanner(System.in);
                input = sin.nextLine();
                if(!input.equals(""))
                    break;
            }
        }
        System.out.println("Program successfully ended.");
        if(totalResultsFlag)
            System.out.println("Some statistics :- The API returned "+totalResultCount+" products in total. For each call, the maximum results obtained is 100.\n"
                    + "Hence there are "+totalResultCount/100+" pages. But of these only "+totalAPICalls+" pages were traversed thereby making only "+(totalAPICalls*100/(double)totalResultCount)+"% of calls.");
    }

       
}

/*
    This class contains the main method
*/
public class Zappos {

    /**
     * @param args the command line arguments
     */    
    
    public static void main(String[] args) {
        
            ZapposProblemSolver problemSolver = new ZapposProblemSolver();
            System.out.println("Enter number of products and total amount ");
            Scanner sin = new Scanner(System.in);
            int no_of_products = sin.nextInt();
            double total_amount = sin.nextDouble();
            
            if(no_of_products>0 && total_amount>0)
            {
                if(problemSolver.initialize())
                {
                    problemSolver.getPriceCombinations(no_of_products, total_amount);
                    problemSolver.getZapposProductDetails();
                    problemSolver.displayZapposProducts();
                }
            }             
            else
                System.out.println("Sorry! No products are found for this input.");
            
    }
}