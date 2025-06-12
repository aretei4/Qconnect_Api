async function openEditor() {
  fetch('http://13.204.49.246:3000/onlyoffice-config')
    .then(res => res.json())
    .then(config => {
	console.log(config);
     new DocsAPI.DocEditor("onlyoffice-editor", config);
    });
}
async function sendData() {
	const urlParams = new URLSearchParams(window.location.search);
  	const fileName = urlParams.get('fileName');
  	const template = urlParams.get('template');
  	console.log('Username:', fileName);
  	console.log('Age:', template);
  	var downladFile = "subash_"+fileName; 
  	if(template == 'new'){
		  downladFile = "new_"+fileName;
	  }else{
		 downladFile = "sample_v01.docx";
	  }
	  console.log('fileName:', downladFile);
	  
      const payload = {
		 // fileUrl: "https://dbqualcy.s3.ap-south-1.amazonaws.com/sampledoc.docx",
		  fileUrl: "http://13.204.49.246:3050/files/"+downladFile,		
        user: {
        id: "Subash",
        name: "Subash Rout",
          fileName: downladFile,
      }
      };

      try {
        const response = await fetch('http://13.204.49.246:3000/api/data', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(payload)
        });

        if (!response.ok) throw new Error('Network response was not ok');
        
        const result = await response.json();
        console.log('Server response:', result);
		 new DocsAPI.DocEditor("onlyoffice-editor", result);
      } catch (error) {
        console.error('Error:', error);
      }
    }
