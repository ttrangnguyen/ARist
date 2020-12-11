import csv
import pandas as pd
from pathlib import Path

# proj = ['ant', 'batik', 'log4j', 'lucene', 'xalan', 'xerces']
proj = ['netbeans', 'eclipse']
typ = ['excode', 'java']
dt = ['train', 'test', 'validate']
data_parent_folders = ['data_csv_6_gram', 'data_csv_7_gram']

for sub_typ in typ:
    for sub_dt in dt:
        for data_parent_folder in data_parent_folders:
            all_filenames = []
            for sub_proj in proj:
                all_filenames.append('../../../../../' + data_parent_folder + '/' + sub_typ + '/' + sub_proj + '/' + sub_typ + '_' +
                                     sub_dt + "_" + sub_proj + '.csv')
            print(all_filenames)
            combined_csv = pd.concat([pd.read_csv(f) for f in all_filenames])
            Path('../../../../../' + data_parent_folder + '/' + sub_typ + '/all/').mkdir(parents=True, exist_ok=True)
            combined_csv.to_csv('../../../../../' + data_parent_folder + '/' + sub_typ + '/all/' + sub_typ + '_' +
                                sub_dt + '_all.csv', index=False, encoding='utf-8-sig')