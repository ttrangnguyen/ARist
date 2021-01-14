import csv
import pandas as pd
from pathlib import Path

# proj = ['ant', 'batik', 'log4j', 'lucene', 'xalan', 'xerces']
projects = ['netbeans', 'eclipse']
data_forms = ['excode']
# data_types = ['train', 'test', 'validate']
data_types = ['fold_' + str(x) for x in range(10)]
data_parent_folders = ['data_csv_3_gram']
data_version = '3'

for data_form in data_forms:
    for data_type in data_types:
        for data_parent_folder in data_parent_folders:
            all_filenames = []
            for project in projects:
                all_filenames.append('../../../../../data_v' + data_version + '/' + data_parent_folder + '/' +
                                     data_form + '/' + project + '/' + data_form + '_' + data_type + "_" + project + '.csv')
            print(all_filenames)
            combined_csv = pd.concat([pd.read_csv(f) for f in all_filenames])
            Path('../../../../../data_v' + data_version + '/' + data_parent_folder + '/' + data_form + '/all/')\
                .mkdir(parents=True, exist_ok=True)
            combined_csv.to_csv('../../../../../data_v' + data_version + '/' + data_parent_folder + '/' + data_form +
                                '/all/' + data_form + '_' + data_type + '_all' + '.csv', index=False, encoding='utf-8-sig')