import csv
import pandas as pd

proj = ['ant', 'batik', 'log4j', 'lucene', 'xalan', 'xerces']
typ = ['excode', 'java']
dt = ['train', 'test', 'validate']
for sub_typ in typ:
    for sub_dt in dt:
        all_filenames = []
        for sub_proj in proj:
            all_filenames.append('../../../../../data_csv/' + sub_typ + '/' + sub_proj + '/' + sub_typ + '_' +
                                 sub_dt + "_" + sub_proj + '.csv')
        print(all_filenames)
        combined_csv = pd.concat([pd.read_csv(f) for f in all_filenames])
        combined_csv.to_csv('../../../../../data_csv/' + sub_typ + '/all/' + sub_typ + '_' +
                            sub_dt + '_all.csv', index=False, encoding='utf-8-sig')