# Distributed Systems Project
## AUEB | Distributed Systems | Semester 6 | 2019 - 2020

The purpose of the project is to create a functional music app for mobile phones, with the help of Android Studio.
For the objectives of the course, the work in the project is distributed to 3 Brokers and 2 Publishers. Also, there is an available dataset with songs 
(indicatively dataset or dataset_, any other dataset works the same).

The application is started by a client / consumer, who asks to listen to a specific song. Clients can only communicate with Brokers, while only Publishers have access to the songs.
Therefore, request is transferred to the competent Broker, who then communicates with the appropriate Publisher. The user can listen to a song in two ways: ON mode and OFF mode.
When the selected mode is OFF, the song is first downloaded in its entirety and then the user can listen to it. When the mode is ON, the song comes in pieces (chunks)
and the user can start listening to it when the first one comes. The connection will not be interrupted, so the whole song will be loaded gradually.
