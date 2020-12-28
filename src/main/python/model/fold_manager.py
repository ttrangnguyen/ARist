import os
import random
from pathlib import Path
from shutil import copyfile


def rand_fold(n_folds):
    return random.randint(0, n_folds - 1)


class FoldManager:

    def __init__(self):
        self.data_forms = ['excode', 'java']
        self.projects = ['netbeans', 'eclipse']
        self.data_types = ['train', 'test', 'validate']
        self.version = '3'
        self.n_folds = 10
        self.data_classform_folder_with_type = ''
        self.fold_paths = []

    def create_n_folds_path(self):
        last_data_form = ''
        for data_form in self.data_forms:
            last_data_form = data_form
            self.fold_paths = []
            self.data_classform_folder_with_type = '../../../../../data_v' + self.version + \
                                                   '/' + 'data_classform/' + data_form + '/'
            n_folds_parent_folder = self.data_classform_folder_with_type + str(self.n_folds) + '_folds' + '/'
            Path(n_folds_parent_folder).mkdir(parents=True, exist_ok=True)
            for i in range(self.n_folds):
                self.fold_paths.append(n_folds_parent_folder + 'fold_' + str(i) + '/')
                Path(self.fold_paths[i]).mkdir(parents=True, exist_ok=True)
                for project in self.projects:
                    Path(self.fold_paths[i] + project).mkdir(parents=True, exist_ok=True)
        return last_data_form

    def create_n_folds(self):
        last_data_form = self.create_n_folds_path()
        for data_type in self.data_types:
            for project in self.projects:
                for r, d, f in os.walk(self.data_classform_folder_with_type + data_type + '/' + project + '/'):
                    for file in f:
                        fold = rand_fold(self.n_folds)
                        file_from = os.path.join(r, file)
                        file_to = self.fold_paths[fold] + project + '/' + file
                        for data_form in self.data_forms:
                            copyfile(file_from.replace(last_data_form, data_form),
                                     file_to.replace(last_data_form, data_form))

    def create_test_path_from_folds(self):
        project_file_paths = []
        for i in range(len(self.projects)):
            project_file_paths.append([])
            for data_type in self.data_types:
                reader = open('../../../../../data_v' + self.version + '/datapath/' +
                              data_type + '/' + self.projects[i] + '.txt', 'r')
                project_file_paths[i] += reader.readlines()
        for fold in range(self.n_folds):
            for i in range(len(self.projects)):
                # Paths of files in that fold
                fold_path_folder = '../../../../../data_v' + self.version + '/datapath/fold_' + str(fold) + '/'
                Path(fold_path_folder).mkdir(parents=True, exist_ok=True)
                writer = open(fold_path_folder + self.projects[i] + '.txt', 'w')
                for r, d, f in os.walk(self.fold_paths[fold] + self.projects[i] + '/'):
                    for file in f:
                        for project_file_path in project_file_paths[i]:
                            if ('\\' + file[:-3] + 'java') in project_file_path:
                                writer.write(project_file_path)
                                break
                writer.close()


if __name__ == "__main__":
    fold_manager = FoldManager()
    # need to run both methods
    fold_manager.create_n_folds()
    fold_manager.create_test_path_from_folds()
