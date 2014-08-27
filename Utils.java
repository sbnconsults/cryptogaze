/*
 *  Name: Utils.java
 *  Description: Holds the util functions used in multiple places
 *
 */
package com.idealtechlabs.cryptogaze;

import java.io.IOException;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;


public class Utils {
	public static final String exchangeAPIUrl = "http://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml";
	public static final String redditFeedUrl = "http://www.reddit.com/r/";
	private static final String[] VALUES_PROJECTION = new String[] {
		DatabaseContract.UNIQUE_ID,
		DatabaseContract.TableValues.COL5,
	};
    public static final Map<String, String> currSymbol;
    static
    {
        currSymbol = new HashMap<String, String>();
		currSymbol.put("USD","$");
		currSymbol.put("INR","`");
		currSymbol.put("EUR",Currency.getInstance("EUR").getSymbol());
		currSymbol.put("CNY",Currency.getInstance(Locale.JAPAN).getSymbol());
		currSymbol.put("JPY",Currency.getInstance(Locale.JAPAN).getSymbol());
    };
    
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	private int getJSONValue(String jsonString, String key) throws JSONException{
		int valInt = 0;
		if(jsonString != null){
			JSONObject object = (JSONObject) new JSONTokener(jsonString).nextValue();
			String valStr = object.getString(key);
			if(valStr != null){ valInt = Integer.parseInt(valStr); }
		}
		
		return valInt;
		
	}
	
	// Function used by updater task
	// Calls the API for specified ticker and returns the current quotes
	public static float getNewValue(String apiUrl, String source, String ticker) throws URISyntaxException, JSONException, ParserConfigurationException, SAXException{
		String newVal = "";

		String response = apiCall(apiUrl);
		JSONObject jo = processJSON(response);

		if(source.equals("BITFINEX")){
			if(ticker.equals("LTC")){
				String last_price = jo.getString("last_price");
				if(last_price != null){ newVal = last_price; }
			}
		}
		else if(source.equals("Bitstamp")){
			String last = jo.getString("last");
			if(last != null){ newVal = last; }			
		}else if(source.equals("BTC-E")){
			JSONObject jo2 = jo.getJSONObject("ticker");
			float buy = Float.parseFloat(jo2.getString("buy"));
			float sell = Float.parseFloat(jo2.getString("sell"));
			newVal = String.valueOf((buy + sell) /2);
		}else if(source.equals("Coinbase")){
			if(ticker.equals("BTC")){
				String amount = jo.getString("amount");
				if(amount != null){ newVal = amount; }
			}
		}else if(source.equals("Crypto-Trade")){
			if(jo.getString("status").equals("success")){
				JSONObject jo2 = jo.getJSONObject("data");
				String last = jo2.getString("last");
				if(last != null){ newVal = last; }
			}
		}
		
		return formatCurrency(Float.parseFloat(newVal));
	}
	
	//Function for getting current exchange rates
	public static HashMap getCurrExchange() throws SAXException, ParserConfigurationException, URISyntaxException{
		String response = apiCall(exchangeAPIUrl);
		return processXML(response, "EXCHNG");
	}
	
	// Function to get the json feed for each coin type
	public static ArrayList getRedditFeed(String coin) throws URISyntaxException, JSONException, ClientProtocolException, IOException{

		ArrayList<HashMap<String, String>> feedList = new ArrayList<HashMap<String, String>>();
           
		String response = apiCall(redditFeedUrl + coin +"/.json");
		JSONObject jo = processJSON(response);
		JSONArray ja = jo.getJSONObject("data").getJSONArray("children");

		for (int i = 0 ; i < ja.length(); i++) {
			JSONObject jobj = ja.getJSONObject(i).getJSONObject("data");
			HashMap<String, String> map = new HashMap<String, String>();
			map.put("headline", jobj.getString("title"));
			map.put("postdate", "Date Posted: "+jobj.getString("created"));
			map.put("url", jobj.getString("url"));
			feedList.add(i,map);
		}
		return feedList;
	}
	
	public static String apiCall(String apiUrl) throws URISyntaxException{
		String result = null;
        // Making HTTP Request
        try {
        	System.out.println(apiUrl);
            HttpClient httpclient = new DefaultHttpClient();
            HttpGet request = new HttpGet();
            URI website = new URI(apiUrl);
            request.setURI(website);

   
            HttpResponse response = httpclient.execute(request);

            HttpEntity responseEntity = response.getEntity();
            if(responseEntity!=null) {
                result = EntityUtils.toString(responseEntity);
            }
        
           
        } catch (Exception e) {
        	e.printStackTrace();
        }
        return result;
	}
    
	public static HashMap processXML(String response, String type) throws SAXException, ParserConfigurationException{
		HashMap<String, Float> exchange = new HashMap<String, Float>();

        if(type.equals("EXCHNG")){
        	try {
                InputSource is=new InputSource();
                is.setCharacterStream(new StringReader(response));
        		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        		DocumentBuilder dbs = dbf.newDocumentBuilder();
        		Document doc = dbs.parse(is);  
        		NodeList nodes = doc.getElementsByTagName("Cube");

        		for(int i=0; i<nodes.getLength(); i++){
        			Node node = nodes.item(i);
        			NamedNodeMap attrs = node.getAttributes();  
    				if(attrs.getLength() == 2){
    					exchange.put( ((Attr)attrs.item(0)).getValue(), Float.parseFloat( ((Attr)attrs.item(1)).getValue() ));

        			}
    			
        		}
 
        	} catch (ClientProtocolException e) {
        		// writing exception to log
        		e.printStackTrace();
        	} catch (IOException e) {
        		// writing exception to log
        		e.printStackTrace();
        	}
        }
        return exchange;
	}
	public static JSONObject processJSON(String response) throws JSONException{
		return new JSONObject(response);
	}
	
	public static float convCurr (HashMap<String, Float> exchange, float value, String oldCurr, String newCurr ){
		if(oldCurr.equals(newCurr)){
			return value;
		}else{
			float newValue;
			if(oldCurr.equals("EUR")){
				newValue = value * exchange.get(newCurr);
			}else if(newCurr.equals("EUR")){
				newValue = value/exchange.get(oldCurr);
			}else{
				
				newValue = value/exchange.get(oldCurr) * exchange.get(newCurr);
			}
			return formatCurrency(newValue);
		}			

	}
	
	public static String getDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
	}
	
	public static float formatCurrency(Float number){
		NumberFormat nf = NumberFormat.getInstance(Locale.US);
		nf.setMaximumFractionDigits(2);
		nf.setMinimumFractionDigits(2);
		String formatted = nf.format(Double.parseDouble(String.valueOf(number)));
		formatted = formatted.replaceAll(",","");
		return Float.parseFloat(formatted);
	}
	
	public static void updateCurrency(Context context, Map<String, String[]> newSetting, boolean isFromSetting) throws SAXException, ParserConfigurationException, URISyntaxException{
		HashMap exchange = null;
		ContentResolver cResolver = context.getContentResolver();
    	for (Map.Entry<String, String[]> entry : newSetting.entrySet()){
    		   
    		String coinId = entry.getKey();
    		String[] values = entry.getValue();
    		String oldCurr = values[0];
    		String newCurr = values[1];
    		
    		if(oldCurr.equals(newCurr)){
    			continue;
    		}

    		ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    		NetworkInfo mWifi = connManager.getNetworkInfo(connManager.getNetworkPreference());
			
			if (mWifi.isConnected()) {
    			if(exchange == null){
    				exchange = Utils.getCurrExchange(); }
    		
    			float newTotal = 0;
    			Cursor valueCursor = cResolver.query(MyContentProvider.VALUES_CONTENT_URI, VALUES_PROJECTION, 
    				DatabaseContract.TableValues.COL1 + " = " + coinId, null, null);
    		
    			if(valueCursor.moveToFirst()){
    				do {
    					int valueId = valueCursor.getInt(0);
    					float oldVal = valueCursor.getFloat(1);
    					float newVal;
					
    					try{
    						newVal = Utils.convCurr(exchange, oldVal, oldCurr, newCurr );
    					}
    					catch(Exception e){
    						continue;
    					}
    					newTotal += newVal;
    					ContentValues valuesNew = new ContentValues();
    					valuesNew.put(DatabaseContract.TableValues.COL4, newCurr);
    					valuesNew.put(DatabaseContract.TableValues.COL5, newVal);
    					valuesNew.put(DatabaseContract.TableValues.COL7, Utils.getDateTime());
    					cResolver.update(Uri.withAppendedPath(MyContentProvider.VALUES_CONTENT_URI,
							String.valueOf(valueId)), valuesNew, null,null);

					} while (valueCursor.moveToNext());
				} 
				valueCursor.close();
				float newAvg = 0;
				if(valueCursor.getCount() != 0){
					newAvg = newTotal / valueCursor.getCount();
				}
				newAvg = Float.parseFloat(String.format("%.2f", newAvg));
				
				ContentValues coinNew = new ContentValues();
				coinNew.put(DatabaseContract.TableCoin.COL3, newCurr);
				coinNew.put(DatabaseContract.TableCoin.COL4, newAvg);
				coinNew.put(DatabaseContract.TableCoin.COL6, Utils.getDateTime());
				coinNew.put(DatabaseContract.TableCoin.COL7, newCurr);
				cResolver.update(Uri.withAppendedPath(MyContentProvider.COINS_CONTENT_URI,
					String.valueOf(coinId)), coinNew, null,null);
    		}else if(isFromSetting){
				ContentValues coinNew = new ContentValues();
				coinNew.put(DatabaseContract.TableCoin.COL7, newCurr);				
				cResolver.update(Uri.withAppendedPath(MyContentProvider.COINS_CONTENT_URI,
						String.valueOf(coinId)), coinNew, null,null);   			
    		}
	
    	}

	}

}
