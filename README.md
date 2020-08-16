# Distributed Systems Project
## AUEB | Distributed Systems | Semester 6 | 2019 - 2020

The purpose of the project is to create a functional music app for mobile phones, with the help of Android Studio.
For the objectives of the course, the work in the project is distributed to 3 Brokers and 2 Publishers. Also, there is an available dataset with songs 
(indicatively dataset or dataset_, any other dataset works the same).

The application is started by a client / consumer, who asks to listen to a specific song. Clients can only communicate with Brokers, while only Publishers have access to the songs. Therefore, request is transferred to the competent Broker, who then communicates with the appropriate Publisher. 

The user can listen to a song in two ways: ON mode and OFF mode. When the selected mode is OFF, the song is first downloaded in its entirety and then the user can listen to it. 
Ιn this case, the song is saved in the list of downloaded songs so that the user can listen to it in the future without having to ask for it again. When the mode is ON, the song comes in pieces (chunks) and the user can start listening to it when the first one comes. The connection will not be interrupted, so the whole song will be loaded gradually.

In order for the project to work, "ProjectKatanem" needs to be opened in an IDE such as Intellij and "MyMusicApp" in Android Studio. At first, the three Brokers need to run and then the two Publishers **, who will read the dataset with the available songs and connect with Brokers. Afterward, Brokers will be ready for client connections through the application (Brokers and Publishers must be running continuously and simultaneously throughout the use of the application). 

Note: Some information will need to be changed if the project is tested on a different computer (the dataset path, the local computer address)

** Theoretically Brokers and Publishers would run on different computers, but to control the project on one computer, they have the address of this one computer and different communication ports. So, when each Broker and Publisher is running, the ports must be different. In my tests, I changed lines 38 - 39 for each Broker before running (first: 1100, 2100, second: 1200, 2200, third: 1300, 2300). This numbers and the local address of my computer are also shown in ProjectKatanem\data\brokers.txt, which Publishers read in order to know the available Brokers and then to connect with them. In a similar way, for Publishers, one uses lines 40 - 41 and the other 42 - 43 (the order in which they run does not matter). Changes in addresses or ports can cause changes in some parts of the code, such as in the given info for the initial connection with a broker in Android studio. 
