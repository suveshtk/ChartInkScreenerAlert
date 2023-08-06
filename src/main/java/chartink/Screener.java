package chartink;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Screener {

	public static void main(String[] args) throws Exception {
		
		String apiToken = System.getenv("API_KEY_TELEGRAM_BOT_CHARTINK");
	    String chatId = System.getenv("TELEGRAM_CLIENT_ID");
	    
		List<String> scrips = screenStocks();
		List<String> scripsFiltered = filterStocksBasedOnFundamentals(scrips);
		
		LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH);
        String formattedDate = currentDate.format(formatter);
		
        String message = "*****" + formattedDate + "******\n";
        
        for(int i=0;i<scripsFiltered.size();i++) {
        	message = message + scripsFiltered.get(i) + "\n";
        }
		System.out.println(message);
		sendTelegramNotification(apiToken, chatId, message);
	}

	public static List<String> screenStocks() {
		String postUrl = "https://chartink.com/screener/process";
		List<String> scrips = new ArrayList<>();

		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			HttpPost httpPost = new HttpPost(postUrl);

			// Set the JSON body
			String requestBody = "scan_clause=(+%7Bcash%7D+(+(+%7Bcash%7D+(+(+(+1+month+ago+close+%2B+1+month+ago+high+%2B+1+month+ago+low+)+%2F+3+-+(+(+1+month+ago+high+%2B+1+month+ago+low+)+%2F+2+)+%2B+(+1+month+ago+close+%2B+1+month+ago+high+%2B+1+month+ago+low+)+%2F+3+)+%3C+latest+close+and(+(+1+month+ago+close+%2B+1+month+ago+high+%2B+1+month+ago+low+)+%2F+3+-+(+(+1+month+ago+high+%2B+1+month+ago+low+)+%2F+2+)+%2B+(+1+month+ago+close+%2B+1+month+ago+high+%2B+1+month+ago+low+)+%2F+3+)+%3C+latest+open+and+latest+ema(+latest+close+%2C+21+)+%3C+latest+close+and+latest+ema(+latest+close+%2C+21+)+%3C+latest+open+and+latest+ema(+latest+close+%2C+30+)+%3C+latest+close+and+latest+ema(+latest+close+%2C+30+)+%3C+latest+open+and+latest+ema(+latest+close+%2C+50+)+%3C+latest+close+and+latest+ema(+latest+close+%2C+50+)+%3C+latest+open+and+latest+rsi(+14+)+%3E+60+and+quarterly+net+profit%2Freported+profit+after+tax+%3E+1+quarter+ago+net+profit%2Freported+profit+after+tax+and+1+quarter+ago+net+profit%2Freported+profit+after+tax+%3E+2+quarter+ago+net+profit%2Freported+profit+after+tax+and(+%7Bcash%7D+(+(+latest+close+-+(+(+1+month+ago+high+%2B+1+month+ago+low+)+%2F+2+)+%2F+latest+close+)+*+100+%3C+5+and(+latest+close+-+latest+ema(+latest+close+%2C+21+)+)+%2F+latest+close+*+100+%3C+5+and(+latest+close+-+latest+ema(+latest+close+%2C+30+)+)+%2F+latest+close+*+100+%3C+5+and(+latest+close+-+latest+ema(+latest+close+%2C+50+)+)+%2F+latest+close+*+100+%3C+5+)+)+and+yearly+debt+equity+ratio+%3C+1.5+and+quarterly+eps+after+extraordinary+items+basic+%3E+1+quarter+ago+eps+after+extraordinary+items+basic+and+1+quarter+ago+eps+after+extraordinary+items+basic+%3E+2+quarter+ago+eps+after+extraordinary+items+basic+and+yearly+net+sales+%3E+1+year+ago+net+sales+)+)+and(+(+1+month+ago+close+%2B+1+month+ago+high+%2B+1+month+ago+open+)+%2F+3+)+%3E+(+(+2+months+ago+close+%2B+2+months+ago+high+%2B+2+months+ago+low+)+%2F+3+)+)+)+";
			StringEntity entity = new StringEntity(requestBody);
			httpPost.setEntity(entity);

			// Set headers
			httpPost.addHeader("content-type", "application/x-www-form-urlencoded; charset=UTF-8");
			httpPost.addHeader("x-csrf-token", "s6chsvB0eUj4ERZp0gOLzgp9K4g8EjpoGVgwABfx");
			httpPost.addHeader("cookie",
					"_ga=GA1.2.65505146.1678679344; remember_web_59ba36addc2b2f9401580f014c7f58ea4e30989d=eyJpdiI6ImIvTmNYQ3drcG9nWDlBb0dXNVFlRWc9PSIsInZhbHVlIjoiTDlGN1BmRlZ5NnhORk4wbWYrSHRKNFZ1eXluL2NIZmxMS1BzMGlBYTl2SGJrWFZtVExGWkpxKzhRQko4VEovczQyZGsySlZrNWQ0anIreVBOMnlvWi9NRldES1VUS3ZpeWpxOENETFgzSW8rQ242UDViZ3FzVHQ5RC9FZ29mQS9ScXQ5NUF1bysrb1BGN3d6OWMwZzhzNVNOWlR0SVNTc04zMWhiWnUrNTlQRFRVOGZLcjd3QWJFeWZMWTUyaThVRzBiZlZtYmdXZENEY09VL2dpaUFDcHI2dk1YK2V1eEJPQ0lkNmM2WEFDdz0iLCJtYWMiOiI1ZWRkNWE0NzVlZWRmZDFjNDc1ZjRlZTkxZjUyNGJiYTA4MGM2MDVkOTVlOGJiYjY4Mzk1ZjY2MzI4YTk3N2I0IiwidGFnIjoiIn0%3D; _gid=GA1.2.480146942.1690814209; _cc_id=e78cbff62fe25250638f8ebd782ab86a; panoramaId_expiry=1691921646617; panoramaId=bd266bdda41d015ebb83008460be4945a70249d849f2f6d2d3723d58887b5a08; panoramaIdType=panoIndiv; cto_bundle=IlxEVl9IVGVHQ1R3OHBqcU41VnNFUjRUdUFtNyUyQmRQRjZrOUhOcExoZW44WmR6TEozRjJZUSUyQkpiSWtXendTU01VSWNaZGxXSktGSVZnNnpsOEJiSmwlMkZGdXlyU21ZTm9pY21lb0FHZjdNd3hkWk1tVE43czhySW9tT0d3c0ZCSFNDMWNLUDZhUjRIMVBpNWQ0a0pOdlElMkIzY01HZW91dmVHTzlnT3JIZGx5ejNCdCUyRnhEbUtWY2RXMWR6STFoZVVRaEFaZ1g5bkF4YllzNkZCOCUyQkRvd2IxYyUyRjBHUkElM0QlM0Q; _ga_7P3KPC3ZPP=GS1.2.1691316832.37.1.1691320338.0.0.0; __gads=ID=fb31112cc3f6e882-22150979f1db00bc:T=1678679344:RT=1691320338:S=ALNI_MaczTxkZYRs6EeK5kv_vzz9AX56Zw; __gpi=UID=00000bd875c006e5:T=1678679344:RT=1691320338:S=ALNI_MbxzDy2tNVeSb-FGcJAt3IPahit4Q; XSRF-TOKEN=eyJpdiI6IjY5ZEhjbmk0bG1vU2l2K2dIVkpYaEE9PSIsInZhbHVlIjoiUzlHa0J3V29nUXhXckRrZk5aY1I0OUxsZDRDV2xSbHM5eHdvcSt1YWFDb2dVcVJZMkwzdWpPajF5cktRaVNXWWVoTG40eklqNVVzWDB3MW1IWE9kZDk4blorbGZiYThhN0swT2dPVFN1OWxqbDU1VTQ1b0l1a2hyRFQxWXJzeGciLCJtYWMiOiJiMDM4Y2Y3Y2E0MWEyNzY2OTI2NDVkODM4ZTE2MDZhZjc5ZjQyZDMxZWRkNjM0M2EzNTA4OGIzYzI4NTkwMGU2IiwidGFnIjoiIn0%3D; ci_session=eyJpdiI6IkR1L05JRDQwblI2R0JsUFkyTXhnb1E9PSIsInZhbHVlIjoiU0pHaEV1U3JWeDBSN1g0T2plNGN4eml6TFhONkxWQXcxV2tFTTcxTmk4WTJ5MUJyTmlGN2RjWUZjc0xFZnhhM0hYTUVCUjh3aU5Ib0VCdDJNeXBrN0dZWUx0ZklQWWYxV1kwODN6WHFhdmRrVExuOXMwUU9uNk9mYXlkcHlOaWYiLCJtYWMiOiIxNmVlNDEzZmVmMTlhNzJmMWQ0MDk1MDAzMGE2M2RiNGZjODA0OWRlMTNmYmRjMjVkNGQyZTdlZjNjNWFkMGEwIiwidGFnIjoiIn0%3D");

			try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
				String responseBody = EntityUtils.toString(response.getEntity());

				ObjectMapper objectMapper = new ObjectMapper();
				JsonNode jsonResultObject = objectMapper.readTree(responseBody);
				JsonNode jsonArrayNode = jsonResultObject.get("data");

				if (jsonArrayNode.isArray()) {
					for (JsonNode jsonNode : jsonArrayNode) {
						scrips.add(jsonNode.get("nsecode").asText());
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return scrips;
	}

	public static void sendTelegramNotification(String apiKey, String chatId, String message) {
		try {
			URL url = new URL("https://api.telegram.org/bot" + apiKey + "/sendMessage");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);

			String postData = "chat_id=" + chatId + "&text=" + message;
			byte[] postDataBytes = postData.getBytes(StandardCharsets.UTF_8);

			try (OutputStream os = conn.getOutputStream()) {
				os.write(postDataBytes);
			}

			int responseCode = conn.getResponseCode();
			System.out.println("Response Code: " + responseCode);

			conn.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static List<String> filterStocksBasedOnFundamentals(List<String> scrips) {

		List<String> scripsFiltered = new ArrayList<>();
		List<Integer> ignoreRows = new ArrayList<>();
		ignoreRows.add(2);
		ignoreRows.add(5);
		ignoreRows.add(6);
		ignoreRows.add(7);
		ignoreRows.add(12);
		ignoreRows.add(9);
		boolean status = true;

		for (int i = 0; i < scrips.size(); i++) {

			status = true;

			try {
				// Replace this with the URL of the HTML page you want to parse
				String url = "https://www.screener.in/company/" + scrips.get(i);

				// Fetch the HTML content from the URL
				Document document = Jsoup.connect(url).get();

				// Locate the table element based on its class or ID
				Element table = document.select("table.data-table").first();

				if (table != null) {
					// Select all rows within the table
					Elements rows = table.select("tr");

					// Loop through rows (skip header row if necessary)
					for (int j = 1; j < rows.size(); j++) {
						if (!ignoreRows.contains(j)) {
							Element row = rows.get(j);
							Elements columns = row.select("td"); // Select cells within the row

							// Fetch details from the columns
							String lastQuarterValue = columns.get(columns.size() - 1).text().replaceAll(",", "")
									.replaceAll("%", "");
							String secondLastQuarterValue = columns.get(columns.size() - 2).text().replaceAll(",", "")
									.replaceAll("%", "");

							double lastQuarterIntValue = Double.parseDouble(lastQuarterValue);
							double secondLastQuarterIntValue = Double.parseDouble(secondLastQuarterValue);

							if (lastQuarterIntValue < secondLastQuarterIntValue || lastQuarterIntValue < 0) {
								status = false;
								break;
							}
						}
					}

					if (status) {
						scripsFiltered.add(scrips.get(i));
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		return scripsFiltered;
	}
}