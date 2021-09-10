from bs4 import BeautifulSoup
import requests
import csv
import pandas

page_link_prefix = 'https://statisticalatlas.com/school-district/California/'
# Full URL: page_link = https://www.melissa.com/v2/lookups/latlngzip4/?lat=33.98209&lng=-118.38494


# Our lists of districts
df = pandas.read_csv('/Users/blujmbp/Dropbox/School/IEOR142/Project/ussd17.csv', encoding = "ISO-8859-1")
df['Name']= df['Name'].replace(' ', '-', regex=True)
ca_df = df

print(ca_df)

new_df = pandas.DataFrame().reindex_like(df)
new_df['Zip'] = 'default'
new_df = new_df[0:0]
print(new_df)

for dist in ca_df.index:
    page_link = page_link_prefix + ca_df['Name'][dist] + '/Overview'
    page_response = requests.get(page_link, timeout=5)
    page_content = BeautifulSoup(page_response.content, "lxml")
    #we use the html parser to parse the url content and store it in a variable.
    
    zips = page_content.select("a[href*=zip]")
    
    for i in range(1, len(zips)):
        print(zips[i].text)
        print(ca_df.iloc[[dist]])
        new_df = new_df.append(ca_df.iloc[[dist]], sort=False)
        new_df.iloc[-1, new_df.columns.get_loc('Zip')] = zips[i].text
        print(new_df)


    new_df.to_csv('/Users/blujmbp/Dropbox/School/IEOR142/Project/ussd17_zips.csv')
