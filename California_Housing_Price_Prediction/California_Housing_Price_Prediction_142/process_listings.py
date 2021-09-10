from bs4 import BeautifulSoup
from Naked.toolshed.shell import execute_js, muterun_js
import requests
import csv
import pandas

df = pandas.read_csv('/Users/blujmbp/Dropbox/School/IEOR142/Project/LA_listings.csv')
df['zip'] = 'default'

for ind in df.index:
    params = '~/Dropbox/School/IEOR142/Project/coord_2_zip.js ' + str(df['latitude'][ind]) + ' ' + str(df['longitude'][ind])
    success = muterun_js(params)
    df['zip'][ind] = int(success.stdout)

df.to_csv('/Users/blujmbp/Dropbox/School/IEOR142/Project/LA_listings_new.csv')