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
  	const srcfile = urlParams.get('srcfile');
  	const destfile = urlParams.get('destfile');
  	console.log('srcfile:', srcfile);
  	console.log('destfile:', destfile);
  	var downladFile = destfile; 
  	if(srcfile == 'new'){
		  downladFile = "new_template.docx";
	  }else{
		 downladFile =srcfile;
	  }
	  console.log('fileName:', downladFile);
	  
      const payload = {
		 // fileUrl: "https://dbqualcy.s3.ap-south-1.amazonaws.com/sampledoc.docx",
		  fileUrl: "http://13.204.49.246:3050/files/"+downladFile,		
		   destFile:destfile,
        user: {
        id: "Subash",
        name: "Subash Rout",
       // fileName: destfile,
      }
      };

      try {
        const response = await fetch('http://13.204.49.246:3050/onlyoffice/config', {
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
